/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.util.Optional.of;
import static java.time.ZoneId.systemDefault;
import static javax.mail.search.ComparisonTerm.GE;
import static javax.mail.search.ComparisonTerm.LE;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailListException;
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
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;
import javax.mail.search.AndTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.FlagTerm;
import java.util.Date;
import java.time.LocalDateTime;


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
  protected java.util.Optional<IMAPEmailPredicateBuilder> getPredicateBuilder() {
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
  protected Message[] getMessages(Folder openFolder) {
    try {
      IMAPEmailPredicateBuilder matcher = getPredicateBuilder().orElseGet(() -> new IMAPEmailPredicateBuilder());

      SearchTerm answeredTerm = getSearchTerm(Flags.Flag.ANSWERED, matcher.getAnswered(), false);
      SearchTerm deletedTerm = getSearchTerm(Flags.Flag.DELETED, matcher.getDeleted(), false);
      SearchTerm recentTerm = getSearchTerm(Flags.Flag.RECENT, matcher.getRecent(), false);
      SearchTerm seenTerm = getSearchTerm(Flags.Flag.SEEN, matcher.getSeen(), false);

      AndTerm searchTerm = new AndTerm(answeredTerm, deletedTerm);
      searchTerm = new AndTerm(searchTerm, recentTerm);
      searchTerm = new AndTerm(searchTerm, seenTerm);

      if (matcher.getReceivedSince() != null) {
        Date receivedSinceDate = convertLocalDateTimeToDate(imapMatcher.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(GE, receivedSinceDate);
        searchTerm = new AndTerm(searchTerm, receivedDateTerm);
      }

      if (matcher.getReceivedUntil() != null) {
        Date receivedUntilDate = convertLocalDateTimeToDate(imapMatcher.getReceivedUntil());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(LE, receivedUntilDate);
        searchTerm = new AndTerm(searchTerm, receivedDateTerm);
      }

      if (matcher.getSentSince() != null) {
        Date sentSinceDate = convertLocalDateTimeToDate(imapMatcher.getSentSince());
        SentDateTerm sentDateTerm = new SentDateTerm(GE, sentSinceDate);
        searchTerm = new AndTerm(sentDateTerm, searchTerm);
      }

      if (matcher.getSentUntil() != null) {
        Date sentUntilDate = convertLocalDateTimeToDate(imapMatcher.getSentUntil());
        SentDateTerm sentDateTerm = new SentDateTerm(LE, sentUntilDate);
        searchTerm = new AndTerm(searchTerm, sentDateTerm);
      }

      return openFolder.search(searchTerm);
    } catch (MessagingException e) {
      LOGGER.error("Error occurred while retrieving emails: {}", e);
      throw new EmailListException("Error retrieving emails: " + e.getMessage(), e);
    }
  }

  private Date convertLocalDateTimeToDate(LocalDateTime date) {
    return Date.from(date.atZone(systemDefault()).toInstant());
  }

  private SearchTerm getSearchTerm(Flags.Flag flag, EmailFilterPolicy policy, boolean defaultValue) {
    if (policy == null) {
      return new FlagTerm(new Flags(flag), defaultValue);
    }
    return new FlagTerm(new Flags(flag), policy.asBoolean().orElse(defaultValue));
  }

}
