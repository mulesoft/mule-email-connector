/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static javax.mail.Folder.READ_ONLY;
import static javax.mail.Folder.READ_WRITE;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.exception.EmailListException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.internal.util.StoredEmailContent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;


/**
 * {@link PagingProvider} implementation for list emails operation.
 *
 * @since 1.0
 */
public final class PagingProviderEmailDelegate<T extends BaseEmailAttributes>
    implements PagingProvider<MailboxConnection, Result<StoredEmailContent, T>> {

  private final MailboxAccessConfiguration configuration;
  private final int pageSize;
  private final List<BaseEmailAttributes> emailsToBeDeleted;
  private Folder folder;
  private final String folderName;
  private int bottom;
  private int top;
  private int limit;
  private final StreamingHelper streamingHelper;
  private int retrievedEmailCount;
  private final boolean deleteAfterRetrieve;
  private final BiConsumer<MailboxConnection, BaseEmailAttributes> deleteAfterReadCallback;
  private final Predicate<BaseEmailAttributes> matcher;
  private boolean initialized = false;

  /**
   * @param configuration           The {@link MailboxAccessConfiguration} associated to this operation.
   * @param folderName              the name of the folder where the emails are stored.
   * @param matcherBuilder          a {@link Predicate} of {@link BaseEmailAttributes} used to filter the output list
   * @param pageSize                size of the block that would be retrieved from the email server. This page doesn't represent the page size to
   *                                be returned by the {@link PagingProvider} because emails must be tested against the {@link BaseEmailPredicateBuilder}
   *                                matcher after retrieval to see if they fulfill matcher's condition.
   * @param limit                   The maximum amount of emails that will be retrieved by htis {@link PagingProvider}
   * @param deleteAfterRetrieve     whether the emails should be deleted after retrieval
   * @param deleteAfterReadCallback callback for deleting each email
   */
  public PagingProviderEmailDelegate(MailboxAccessConfiguration configuration, String folderName,
                                     BaseEmailPredicateBuilder matcherBuilder,
                                     int pageSize,
                                     int limit,
                                     boolean deleteAfterRetrieve,
                                     BiConsumer<MailboxConnection, BaseEmailAttributes> deleteAfterReadCallback,
                                     StreamingHelper streamingHelper) {
    this.configuration = configuration;
    this.folderName = folderName;
    this.matcher = matcherBuilder != null ? matcherBuilder.build() : e -> true;
    this.pageSize = pageSize;
    this.top = pageSize;
    this.limit = limit;
    this.streamingHelper = streamingHelper;
    this.retrievedEmailCount = 0;
    this.deleteAfterRetrieve = deleteAfterRetrieve;
    this.deleteAfterReadCallback = deleteAfterReadCallback;
    this.emailsToBeDeleted = new LinkedList<>();
  }

  /**
   * Retrieves emails numbered from {@code bottom} up to {@code top} in the specified {@code folderName}.
   * <p>
   * A new {@link Result} is created for each fetched email from the folder, where the payload is the text body of the email and
   * the other metadata is carried by an {@link BaseEmailAttributes} instance.
   * <p>
   * For folder implementations (like IMAP) that support fetching without reading the content, if the content should NOT be read
   * ({@code shouldReadContent} = false) the SEEN flag is not going to be set. If {@code deleteAfterRead} flag is set to true, the
   * callback {@code deleteAfterReadCallback} is applied to each email.
   */
  private List<Result<StoredEmailContent, T>> list(int startIndex, int endIndex) {

    try {
      List<Result<StoredEmailContent, T>> emails = new LinkedList<>();
      for (Message message : folder.getMessages(startIndex, endIndex)) {
        StoredEmailContent content = StoredEmailContent.EMPTY;
        T attributes = configuration.parseAttributesFromMessage(message, folder);
        if (matcher.test(attributes)) {
          if (configuration.isEagerlyFetchContent()) {
            content = new StoredEmailContent(message, streamingHelper);
            // Attributes are parsed again since they may change after the email has been read.
            attributes = configuration.parseAttributesFromMessage(message, folder);
          }
          emails.add(Result.<StoredEmailContent, T>builder()
              .output(content)
              .attributes(attributes)
              .build());
          if (deleteAfterRetrieve) {
            emailsToBeDeleted.add(attributes);
          }
        }
      }
      return emails;
    } catch (Exception e) {
      throw new EmailListException("Error while retrieving emails: " + e.getMessage(), e);
    }
  }

  @Override
  public List<Result<StoredEmailContent, T>> getPage(MailboxConnection connection) {

    if (limit > 0 && retrievedEmailCount >= limit) {
      return emptyList();
    }

    try {
      folder = connection.getFolder(folderName, deleteAfterRetrieve ? READ_WRITE : READ_ONLY);

      // initialize mailbox indexes
      if (!initialized) {
        initialized = true;
        top = folder.getMessageCount();
        bottom = max(1, top - pageSize + 1);

        if (top == 0)
          return emptyList();
      }

      while (bottom <= top && (limit < 0 || retrievedEmailCount < limit) && bottom >= 1) {
        List<Result<StoredEmailContent, T>> emails = list(bottom, top);

        top -= pageSize;
        bottom = max(1, top - pageSize + 1);

        int retrievedPageSize = emails.size();
        retrievedEmailCount += retrievedPageSize;
        if (retrievedPageSize > 0) {
          // if limited and the limit was exceeded, return the amount that is left until reaching the limit
          int limitedPage =
              limit > 0 && retrievedEmailCount > limit ? min(retrievedPageSize, retrievedEmailCount - limit) : retrievedPageSize;
          emails = emails.subList(0, limitedPage);
          reverse(emails);
          return emails;
        }
      }

    } catch (MessagingException e) {
      throw new EmailException("Error while retrieving emails: ", e);
    }

    return emptyList();
  }

  /**
   * @param connection The connection to be used to do the query.
   * @return {@link Optional#empty()} because a priori there is no way for knowing how many emails are going to be tested
   * {@code true} against the {@link BaseEmailPredicateBuilder} matcher.
   */
  @Override
  public Optional<Integer> getTotalResults(MailboxConnection connection) {
    return Optional.empty();
  }

  @Override
  public void close(MailboxConnection connection) throws MuleException {
    emailsToBeDeleted.forEach(e -> deleteAfterReadCallback.accept(connection, e));
    connection.closeFolder(true);
  }

  @Override
  public boolean useStickyConnections() {
    return true;
  }
}
