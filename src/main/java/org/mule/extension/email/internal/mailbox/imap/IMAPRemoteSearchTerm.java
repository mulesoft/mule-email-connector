/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import org.mule.extension.email.api.predicate.EmailFilterPolicy;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.slf4j.Logger;

import static java.util.Optional.of;
import static java.util.Optional.empty;
import static java.time.ZoneId.systemDefault;
import static org.slf4j.LoggerFactory.getLogger;
import static javax.mail.Flags.Flag.ANSWERED;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.search.ComparisonTerm.GE;
import static javax.mail.search.ComparisonTerm.LE;

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

public class IMAPRemoteSearchTerm {

  private final static Logger LOGGER = getLogger(IMAPRemoteSearchTerm.class);

  private IMAPEmailPredicateBuilder imapEmailPredicateBuilder;
  private java.util.Optional<SearchTerm> remoteSearchTerm = empty();

  public IMAPRemoteSearchTerm(IMAPEmailPredicateBuilder predicateBuilder) {
    this.imapEmailPredicateBuilder = predicateBuilder;

    HashMap<Flags.Flag, Supplier<EmailFilterPolicy>> flagMatcherMap = new HashMap();

    flagMatcherMap.put(ANSWERED, () -> this.imapEmailPredicateBuilder.getAnswered());
    flagMatcherMap.put(DELETED, () -> this.imapEmailPredicateBuilder.getDeleted());
    flagMatcherMap.put(RECENT, () -> this.imapEmailPredicateBuilder.getRecent());
    flagMatcherMap.put(SEEN, () -> this.imapEmailPredicateBuilder.getSeen());

    try {
      this.remoteSearchTerm = buildSearchFilter(flagMatcherMap);

      if (!this.remoteSearchTerm.isPresent()) {
        return;
      }

      List<SearchTerm> dateAndRegexFilters = new ArrayList<>();

      if (this.imapEmailPredicateBuilder.getSubjectRegex() != null) {
        SubjectTerm subjectTerm = new SubjectTerm(this.imapEmailPredicateBuilder.getSubjectRegex());
        dateAndRegexFilters.add(subjectTerm);
      }

      if (this.imapEmailPredicateBuilder.getReceivedSince() != null) {
        Date receivedSinceDate = convertLocalDateTimeToDate(this.imapEmailPredicateBuilder.getReceivedSince());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(GE, receivedSinceDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (this.imapEmailPredicateBuilder.getReceivedUntil() != null) {
        Date receivedUntilDate = convertLocalDateTimeToDate(this.imapEmailPredicateBuilder.getReceivedUntil());
        ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(LE, receivedUntilDate);
        dateAndRegexFilters.add(receivedDateTerm);
      }

      if (this.imapEmailPredicateBuilder.getSentSince() != null) {
        Date sentSinceDate = convertLocalDateTimeToDate(this.imapEmailPredicateBuilder.getSentSince());
        SentDateTerm sentDateTerm = new SentDateTerm(GE, sentSinceDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (this.imapEmailPredicateBuilder.getSentUntil() != null) {
        Date sentUntilDate = convertLocalDateTimeToDate(this.imapEmailPredicateBuilder.getSentUntil());
        SentDateTerm sentDateTerm = new SentDateTerm(LE, sentUntilDate);
        dateAndRegexFilters.add(sentDateTerm);
      }

      if (this.imapEmailPredicateBuilder.getFromRegex() != null) {
        FromStringTerm fromTerm = new FromStringTerm(this.imapEmailPredicateBuilder.getFromRegex());
        dateAndRegexFilters.add(fromTerm);
      }

      if (!dateAndRegexFilters.isEmpty()) {
        SearchTerm[] additionalFiltersArray = new SearchTerm[dateAndRegexFilters.size()];
        dateAndRegexFilters.toArray(additionalFiltersArray);
        this.remoteSearchTerm = of(new AndTerm(this.remoteSearchTerm.get(), new AndTerm(additionalFiltersArray)));
      }

    } catch (Exception e) {
      LOGGER.error("Error occurred building imap matcher. Server side filtering will not be applied. {}", e);
    }
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

  private Date convertLocalDateTimeToDate(LocalDateTime date) {
    return Date.from(date.atZone(systemDefault()).toInstant());
  }

  private FlagTerm getFlagTerm(Flags.Flag flag, boolean setValue) {
    //if no policy defined, then default to INCLUDE
    return new FlagTerm(new Flags(flag), setValue);
  }

  public java.util.Optional<SearchTerm> getRemoteSearchTerm() {
    return this.remoteSearchTerm;
  }
}
