/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.util.Optional.of;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailListException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.api.predicate.EmailFilterPolicy;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.BaseMailboxPollingSource;
import org.mule.extension.email.internal.resolver.StoredEmailContentTypeResolver;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import java.time.LocalDateTime;
import java.time.ZoneId;


/**
 * Retrieves all the emails from an IMAP mailbox folder, watermark can be enabled for polled items.
 *
 * @since 1.1
 */
@DisplayName("On New Email - IMAP")
@Alias("listener-imap")
@MetadataScope(outputResolver = StoredEmailContentTypeResolver.class)
public class IMAPPollingSource extends BaseMailboxPollingSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(IMAPPollingSource.class);

  /**
   * If watermark should be applied to the polled emails or not. Default to true.
   */
  @Parameter
  @DisplayName("Enable Watermark")
  @Optional(defaultValue = "true")
  private boolean watermarkEnabled;

  /**
   * A matcher to filter emails retrieved by this polling source. For default already read emails will be filtered.
   */
  @Parameter
  @Optional
  private IMAPEmailPredicateBuilder imapMatcher;

  /**
   * {@inheritDoc}
   */
  @Override
  protected java.util.Optional<? extends BaseEmailPredicateBuilder> getPredicateBuilder() {
    return of(imapMatcher == null ? new DefaultPollingSourceMatcher() : imapMatcher);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isWatermarkEnabled() {
    return watermarkEnabled;
  }

  @Override
  protected void emailDispatchedToFlow() {
    beginUsingFolder();
  }

  @Override
  public void onRejectedItem(Result<StoredEmailContent, BaseEmailAttributes> result,
                             SourceCallbackContext sourceCallbackContext) {
    endUsingFolder();
    super.onRejectedItem(result, sourceCallbackContext);
  }

  @OnBackPressure
  public void onBackPressure() {
    endUsingFolder();
  }

  @OnTerminate
  public void onTerminate() {
    endUsingFolder();
  }


  @Override
  public void poll(PollContext<StoredEmailContent, BaseEmailAttributes> pollContext) {
    if (isFolderBeingUsed()) {
      LOGGER.debug("Poll will be skipped, since last poll emails are still being processed");
      return;
    }
    try {
      beginUsingFolder();
      for (Message message : getMessages(openFolder, pollContext.getWatermark())) {
        BaseEmailAttributes attributes = config.parseAttributesFromMessage(message, openFolder);
        String id = attributes.getId();
        emailDispatchedToFlow();
        pollContext.accept(item -> {
          if (isWatermarkEnabled()) {
            item.setWatermark(Long.valueOf(id));
          }
          item.setId(id);
          item.setResult(Result.<StoredEmailContent, BaseEmailAttributes>builder()
              .output(getEmailContent(message, id))
              .attributes(attributes)
              .build());

          if (deleteAfterRetrieve) {
            markAsDeleted(id, message);
          }
        });
      }

    } catch (MessagingException e) {
      LOGGER.error(e.getMessage(), e);
      //TODO find suitable exception handling
      //pollContext.onConnectionException(e);
    } finally {
      endUsingFolder();
    }
  }


  private Message[] getMessages(Folder openFolder, java.util.Optional<Serializable> watermark) throws MessagingException {
    try {

      //Ver que pasa con los required.
      //estos se resuelven remotos.
      FlagTerm answeredTerm = getFlagTerm(Flags.Flag.ANSWERED, imapMatcher.getAnswered(), true);
      FlagTerm deletedTerm = getFlagTerm(Flags.Flag.DELETED, imapMatcher.getDeleted(), true);
      FlagTerm recentTerm = getFlagTerm(Flags.Flag.RECENT, imapMatcher.getRecent(), true);
      FlagTerm seenTerm = getFlagTerm(Flags.Flag.SEEN, imapMatcher.getSeen(), true);

      AndTerm searchTerm = new AndTerm(answeredTerm, deletedTerm);
      searchTerm = new AndTerm(searchTerm, recentTerm);
      searchTerm = new AndTerm(searchTerm, seenTerm);

      //estos se resuelven locales.
      if (imapMatcher.getReceivedSince() != null) {
        Date receivedSinceDate = convertLocalDateTimeToDate(imapMatcher.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(ComparisonTerm.GE, receivedSinceDate);
        searchTerm = new AndTerm(searchTerm, receivedDateTerm);
      }

      if (imapMatcher.getReceivedUntil() != null) {
        Date receivedUntilDate = convertLocalDateTimeToDate(imapMatcher.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(ComparisonTerm.LE, receivedUntilDate);
        searchTerm = new AndTerm(searchTerm, receivedDateTerm);
      }

      if (imapMatcher.getSentSince() != null) {
        Date sentSinceDate = convertLocalDateTimeToDate(imapMatcher.getSentSince());
        SentDateTerm sentDateTerm = new SentDateTerm(ComparisonTerm.GE, sentSinceDate);
        searchTerm = new AndTerm(searchTerm, sentDateTerm);
      }

      if (imapMatcher.getSentUntil() != null) {
        Date sentUntilDate = convertLocalDateTimeToDate(imapMatcher.getSentUntil());
        SentDateTerm sentDateTerm = new SentDateTerm(ComparisonTerm.GE, sentUntilDate);
        searchTerm = new AndTerm(searchTerm, sentDateTerm);
      }

      //como aparentemente no hay un metodo que permita filtrar por messageID...
      //el watermark es artesanal. Dudo que esto sea confiable... 
      Message[] tmpResult = openFolder.search(searchTerm);

      if (isWatermarkEnabled() && watermark.isPresent()) {
        Long wm = (Long) watermark.get();
        tmpResult = Arrays.stream(tmpResult).filter(x -> Long.valueOf(x.getMessageNumber()) > wm).toArray(Message[]::new);
      }

      /* mejorar error handling adentro de las lambdas.
      if(imapMatcher.getFromRegex()!=null){
          Predicate<String> fromPredicate = compile(imapMatcher.getFromRegex()).asPredicate();
          tmpResult =Arrays.stream(tmpResult).filter(x-> Arrays.stream(x.getFrom()).anyMatch(fromPredicate)).toArray(Message[]::new);
      }
      
      if(imapMatcher.getSubjectRegex()!=null){
        tmpResult =Arrays.stream(tmpResult).filter(x-> x.getSubject().matches(imapMatcher.getSubjectRegex())).toArray(Message[]::new);
      }
      */

      return tmpResult;

    } catch (MessagingException e) {
      throw new EmailListException("Error retrieving emails: " + e.getMessage(), e);
    }
  }



  private Date convertLocalDateTimeToDate(LocalDateTime date) {
    return java.util.Date.from(date
        .atZone(ZoneId.systemDefault()).toInstant());
  }

  private FlagTerm getFlagTerm(Flags.Flag flag, EmailFilterPolicy policy, boolean defaultValue) {
    return new FlagTerm(new Flags(flag), policy.asBoolean().orElse(defaultValue));
  }

}
