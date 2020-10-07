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
import static javax.mail.Flags.Flag.ANSWERED;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Flags.Flag.RECENT;

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
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.AndTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SubjectTerm;
import javax.mail.search.FromStringTerm;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.Map.Entry;

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
    if (!getPredicateBuilder().isPresent() || !getPredicateBuilder().get().getEnableRemoteSearchFilter()) {
      //Filters will be applied locally.
      return super.getMessages(openFolder);
    }

    IMAPEmailPredicateBuilder matcher = getPredicateBuilder().orElseGet(() -> new IMAPEmailPredicateBuilder());
    HashMap<Flag, Supplier<EmailFilterPolicy>> flagMatcherMap = new HashMap();

    flagMatcherMap.put(ANSWERED, () -> matcher.getAnswered());
    flagMatcherMap.put(DELETED, () -> matcher.getDeleted());
    flagMatcherMap.put(RECENT, () -> matcher.getRecent());
    flagMatcherMap.put(SEEN, () -> matcher.getSeen());

    try {
      SearchTerm searchTerm = buildSearchFilter(flagMatcherMap);

      if (searchTerm == null) {
        return openFolder.getMessages();
      }

      List<SearchTerm> dateAndRegexFilters = new ArrayList<>();

      if (matcher.getSubjectRegex() != null) {
        SubjectTerm subjectTerm = new SubjectTerm(matcher.getSubjectRegex());
        dateAndRegexFilters.add(subjectTerm);
      }

      if (matcher.getReceivedSince() != null) {
        Date receivedSinceDate = convertLocalDateTimeToDate(matcher.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(GE, receivedSinceDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (matcher.getReceivedUntil() != null) {
        Date receivedUntilDate = convertLocalDateTimeToDate(matcher.getReceivedUntil());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(LE, receivedUntilDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (matcher.getSentSince() != null) {
        Date sentSinceDate = convertLocalDateTimeToDate(matcher.getSentSince());
        SentDateTerm sentDateTerm = new SentDateTerm(GE, sentSinceDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (matcher.getSentUntil() != null) {
        Date sentUntilDate = convertLocalDateTimeToDate(matcher.getSentUntil());
        SentDateTerm sentDateTerm = new SentDateTerm(LE, sentUntilDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (matcher.getFromRegex() != null) {
        FromStringTerm fromTerm = new FromStringTerm(matcher.getFromRegex());
        dateAndRegexFilters.add(fromTerm);
      }

      if (!dateAndRegexFilters.isEmpty()) {
        SearchTerm[] additionalFiltersArray = new SearchTerm[dateAndRegexFilters.size()];
        dateAndRegexFilters.toArray(additionalFiltersArray);
        searchTerm = new AndTerm(searchTerm, new AndTerm(additionalFiltersArray));
      }

      try {
        return openFolder.search(searchTerm);
      } catch (MessagingException e) {
        return openFolder.search(buildSearchFilter(flagMatcherMap));
      }
    } catch (MessagingException e) {
      LOGGER.error("Error occurred while retrieving emails: {}", e);
      throw new EmailListException("Error retrieving emails: " + e.getMessage(), e);
    }
  }

  private Date convertLocalDateTimeToDate(LocalDateTime date) {
    return Date.from(date.atZone(systemDefault()).toInstant());
  }

  private FlagTerm getFlagTerm(Flag flag, boolean setValue) {
    //if no policy defined, then default to INCLUDE
    return new FlagTerm(new Flags(flag), setValue);
  }

  private SearchTerm buildSearchFilter(HashMap<Flag, Supplier<EmailFilterPolicy>> flagMatcherMap) {
    AndTerm requireTerm = null;
    OrTerm includeTerm = null;
    NotTerm excludeTerm = null;
    List<FlagTerm> andTerms = new ArrayList<>();
    List<FlagTerm> andNegatedTerms = new ArrayList<>();
    List<FlagTerm> orTerms = new ArrayList<>();

    for (Entry<Flag, Supplier<EmailFilterPolicy>> flagMatcherEntry : flagMatcherMap.entrySet()) {
      EmailFilterPolicy policy = flagMatcherEntry.getValue().get();
      if (policy == null) {
        continue;
      }

      FlagTerm flagTerm = getFlagTerm(flagMatcherEntry.getKey(), true);

      if (!policy.asBoolean().isPresent()) {
        //This is an INCLUDE
        orTerms.add(flagTerm);
      } else if (policy.asBoolean().get().booleanValue()) {
        andTerms.add(flagTerm);
      } else {
        andNegatedTerms.add(flagTerm);
      }
    }

    if (andTerms.isEmpty() && andNegatedTerms.isEmpty() && orTerms.isEmpty()) {
      // No matcher
      return null;
    }

    FlagTerm[] orTermsArray = new FlagTerm[orTerms.size()];
    orTerms.toArray(orTermsArray);
    includeTerm = new OrTerm(orTermsArray);

    if (andTerms.isEmpty() && andNegatedTerms.isEmpty()) {
      return includeTerm;
    }

    FlagTerm[] andTermsArray = new FlagTerm[andTerms.size()];
    andTerms.toArray(andTermsArray);
    requireTerm = new AndTerm(andTermsArray);

    FlagTerm[] negatedAndTermsArray = new FlagTerm[andNegatedTerms.size()];
    andNegatedTerms.toArray(negatedAndTermsArray);
    excludeTerm = new NotTerm(new OrTerm(negatedAndTermsArray));

    if (orTerms.isEmpty()) {
      if (!andNegatedTerms.isEmpty() && !andTerms.isEmpty()) {
        return new AndTerm(requireTerm, excludeTerm);
      }

      if (andNegatedTerms.isEmpty()) {
        return requireTerm;
      }

      return excludeTerm;
    }

    if (andNegatedTerms.isEmpty()) {
      return new OrTerm(includeTerm, requireTerm);
    }

    if (andTerms.isEmpty()) {
      return new OrTerm(includeTerm, excludeTerm);
    }

    return new OrTerm(includeTerm, new AndTerm(excludeTerm, requireTerm));

  }

}
