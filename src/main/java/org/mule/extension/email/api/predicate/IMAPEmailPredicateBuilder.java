/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import static java.util.Optional.of;
import static java.util.Optional.empty;
import static java.time.ZoneId.systemDefault;
import static javax.mail.Flags.Flag.ANSWERED;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.search.ComparisonTerm.GE;
import static javax.mail.search.ComparisonTerm.LE;
import static org.mule.extension.email.api.predicate.EmailFilterPolicy.INCLUDE;
import org.mule.extension.email.api.EmailFlags;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Flags;
import javax.mail.search.SearchTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.AndTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SubjectTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.FromStringTerm;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link IMAPEmailAttributes} instance is compliant with a number of criteria.
 * This builder is stateful and not thread-safe. A new instance should be use per each desired {@link Predicate}.
 * <p>
 * This builder adds the capability to build a predicate that filters by the {@link EmailFlags} contained in
 * an email returned from an IMAP mailbox
 * <p>
 * The class is also given the &quot;imap-matcher&quot; alias to make it DSL/XML friendly.
 *
 * @since 1.0
 */
@TypeDsl(allowTopLevelDefinition = true)
@Alias("imap-matcher")
public class IMAPEmailPredicateBuilder extends BaseEmailPredicateBuilder {

  /**
   * Indicates if should retrieve 'seen' or 'not seen' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy seen;

  /**
   * Indicates if should retrieve 'answered' or 'not answered' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy answered;

  /**
   * Indicates if should retrieve 'marked as deleted' or 'not marked as deleted' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy deleted;

  /**
   * "Indicates if should retrieve 'recent' or 'not recent' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy recent;

  private java.util.Optional<SearchTerm> remoteSearchTerm = empty();

  private final static Logger LOGGER = LoggerFactory.getLogger(IMAPEmailPredicateBuilder.class);

  public void initializeRemoteSearchTerm() {
    HashMap<Flags.Flag, Supplier<EmailFilterPolicy>> flagMatcherMap = new HashMap();

    flagMatcherMap.put(ANSWERED, () -> this.answered);
    flagMatcherMap.put(DELETED, () -> this.deleted);
    flagMatcherMap.put(RECENT, () -> this.recent);
    flagMatcherMap.put(SEEN, () -> this.seen);

    try {
      this.remoteSearchTerm = buildSearchFilter(flagMatcherMap);

      if (!this.remoteSearchTerm.isPresent()) {
        return;
      }

      List<SearchTerm> dateAndRegexFilters = new ArrayList<>();

      if (this.getSubjectRegex() != null) {
        SubjectTerm subjectTerm = new SubjectTerm(this.getSubjectRegex());
        dateAndRegexFilters.add(subjectTerm);
      }

      if (this.getReceivedSince() != null) {
        Date receivedSinceDate = convertLocalDateTimeToDate(this.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(GE, receivedSinceDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (this.getReceivedUntil() != null) {
        Date receivedUntilDate = convertLocalDateTimeToDate(this.getReceivedUntil());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(LE, receivedUntilDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (this.getSentSince() != null) {
        Date sentSinceDate = convertLocalDateTimeToDate(this.getSentSince());
        SentDateTerm sentDateTerm = new SentDateTerm(GE, sentSinceDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (this.getSentUntil() != null) {
        Date sentUntilDate = convertLocalDateTimeToDate(this.getSentUntil());
        SentDateTerm sentDateTerm = new SentDateTerm(LE, sentUntilDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (this.getFromRegex() != null) {
        FromStringTerm fromTerm = new FromStringTerm(this.getFromRegex());
        dateAndRegexFilters.add(fromTerm);
      }

      if (!dateAndRegexFilters.isEmpty()) {
        SearchTerm[] additionalFiltersArray = new SearchTerm[dateAndRegexFilters.size()];
        dateAndRegexFilters.toArray(additionalFiltersArray);
        this.remoteSearchTerm = of(new AndTerm(this.remoteSearchTerm.get(), new AndTerm(additionalFiltersArray)));
      }

    } catch (Exception e) {
      LOGGER.error("Error occurred building imap matcher {}", e);
    }
  }

  @Override
  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {
    Predicate<IMAPEmailAttributes> predicate = imapEmailAttributes -> true;

    if (!INCLUDE.equals(recent)) {
      predicate = predicate.and(attributes -> recent.asBoolean().get() == attributes.getFlags().isRecent());
    }

    if (!INCLUDE.equals(deleted)) {
      predicate = predicate.and(attributes -> deleted.asBoolean().get() == attributes.getFlags().isDeleted());
    }

    if (!INCLUDE.equals(answered)) {
      predicate = predicate.and(attributes -> answered.asBoolean().get() == attributes.getFlags().isAnswered());
    }

    if (!INCLUDE.equals(seen)) {
      predicate = predicate.and(attributes -> seen.asBoolean().get() == attributes.getFlags().isSeen());
    }

    return predicate;
  }

  public EmailFilterPolicy getSeen() {
    return seen;
  }

  public EmailFilterPolicy getAnswered() {
    return answered;
  }

  public EmailFilterPolicy getDeleted() {
    return deleted;
  }

  public EmailFilterPolicy getRecent() {
    return recent;
  }

  public IMAPEmailPredicateBuilder setAnswered(EmailFilterPolicy answered) {
    this.answered = answered;
    return this;
  }

  public IMAPEmailPredicateBuilder setDeleted(EmailFilterPolicy deleted) {
    this.deleted = deleted;
    return this;
  }

  public IMAPEmailPredicateBuilder setRecent(EmailFilterPolicy recent) {
    this.recent = recent;
    return this;
  }

  public IMAPEmailPredicateBuilder setSeen(EmailFilterPolicy seen) {
    this.seen = seen;
    return this;
  }

  public java.util.Optional<SearchTerm> getRemoteSearchTerm() {
    return this.remoteSearchTerm;
  }

  private Date convertLocalDateTimeToDate(LocalDateTime date) {
    return Date.from(date.atZone(systemDefault()).toInstant());
  }

  private FlagTerm getFlagTerm(Flags.Flag flag, boolean setValue) {
    //if no policy defined, then default to INCLUDE
    return new FlagTerm(new Flags(flag), setValue);
  }

  private java.util.Optional<SearchTerm> buildSearchFilter(HashMap<Flags.Flag, Supplier<EmailFilterPolicy>> flagMatcherMap) {
    AndTerm requireTerm = null;
    OrTerm includeTerm = null;
    NotTerm excludeTerm = null;
    List<FlagTerm> andTerms = new ArrayList<>();
    List<FlagTerm> andNegatedTerms = new ArrayList<>();
    List<FlagTerm> orTerms = new ArrayList<>();

    for (Map.Entry<Flags.Flag, Supplier<EmailFilterPolicy>> flagMatcherEntry : flagMatcherMap.entrySet()) {
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
      return empty();
    }

    FlagTerm[] orTermsArray = new FlagTerm[orTerms.size()];
    orTerms.toArray(orTermsArray);
    includeTerm = new OrTerm(orTermsArray);

    if (andTerms.isEmpty() && andNegatedTerms.isEmpty()) {
      return of(includeTerm);
    }

    FlagTerm[] andTermsArray = new FlagTerm[andTerms.size()];
    andTerms.toArray(andTermsArray);
    requireTerm = new AndTerm(andTermsArray);

    FlagTerm[] negatedAndTermsArray = new FlagTerm[andNegatedTerms.size()];
    andNegatedTerms.toArray(negatedAndTermsArray);
    excludeTerm = new NotTerm(new OrTerm(negatedAndTermsArray));

    if (orTerms.isEmpty()) {
      if (!andNegatedTerms.isEmpty() && !andTerms.isEmpty()) {
        return of(new AndTerm(requireTerm, excludeTerm));
      }

      if (andNegatedTerms.isEmpty()) {
        return of(requireTerm);
      }

      return of(excludeTerm);
    }

    if (andNegatedTerms.isEmpty()) {
      return of(new OrTerm(includeTerm, requireTerm));
    }

    if (andTerms.isEmpty()) {
      return of(new OrTerm(includeTerm, excludeTerm));
    }

    return of(new OrTerm(includeTerm, new AndTerm(excludeTerm, requireTerm)));
  }

}
