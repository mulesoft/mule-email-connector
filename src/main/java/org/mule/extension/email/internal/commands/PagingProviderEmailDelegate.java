/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TYPE_HEADER;
import static org.mule.runtime.api.metadata.MediaType.ANY;

import org.mule.extension.email.api.IncomingEmail;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.internal.util.EmailContentProcessor;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import javax.mail.Folder;
import javax.mail.MessagingException;
import java.io.IOException;
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
    implements PagingProvider<MailboxConnection, Result<IncomingEmail, T>> {

  private final MailboxAccessConfiguration configuration;
  private final int pageSize;
  private final String folderName;
  private final Predicate<BaseEmailAttributes> matcher;
  private final BiConsumer<MailboxConnection, BaseEmailAttributes> deleteAfterReadCallback;

  private int bottom = 1;
  private int top;
  private MailboxConnection connection;

  /**
   * @param configuration           The {@link MailboxAccessConfiguration} associated to this operation.
   * @param folderName              the name of the folder where the emails are stored.
   * @param matcherBuilder          a {@link Predicate} of {@link BaseEmailAttributes} used to filter the output list
   * @param pageSize                size of the block that would be retrieved from the email server. This page doesn't represent the page size to
   *                                be returned by the {@link PagingProvider} because emails must be tested against the {@link BaseEmailPredicateBuilder}
   *                                matcher after retrieval to see if they fulfill matcher's condition.
   * @param deleteAfterReadCallback callback for deleting each email
   */
  public PagingProviderEmailDelegate(MailboxAccessConfiguration configuration,
                                     String folderName,
                                     BaseEmailPredicateBuilder matcherBuilder,
                                     int pageSize,
                                     BiConsumer<MailboxConnection, BaseEmailAttributes> deleteAfterReadCallback) {
    this.configuration = configuration;
    this.folderName = folderName;
    this.matcher = matcherBuilder != null ? matcherBuilder.build() : e -> true;
    this.pageSize = pageSize;
    this.top = pageSize;
    this.deleteAfterReadCallback = deleteAfterReadCallback;
  }

  /**
   * Retrieves emails numbered from {@code startIndex} up to {@code top} in the specified {@code folderName}.
   * <p>
   * A new {@link Result} is created for each fetched email from the folder, where the payload is the text body of the email and
   * the other metadata is carried by an {@link BaseEmailAttributes} instance.
   * <p>
   * For folder implementations (like IMAP) that support fetching without reading the content, if the content should NOT be read
   * ({@code shouldReadContent} = false) the SEEN flag is not going to be set. If {@code deleteAfterRead} flag is set to true, the
   * callback {@code deleteAfterReadCallback} is applied to each email.
   */
  private List<Result<IncomingEmail, T>> list(int startIndex, int endIndex, Folder folder) {
    try {
      List<Result<IncomingEmail, T>> retrievedEmails = new LinkedList<>();
      for (javax.mail.Message m : folder.getMessages(startIndex, endIndex)) {
        IncomingEmail email = IncomingEmail.EMPTY;
        T attributes = configuration.parseAttributesFromMessage(m, folder);
        if (matcher.test(attributes)) {
          if (configuration.isEagerlyFetchContent()) {
            EmailContentProcessor processor = EmailContentProcessor.getInstance(m);
            email = new IncomingEmail(processor.getBody(getMediaType(attributes)), processor.getAttachments());
            // Attributes are parsed again since they may change after the email has been read.
            attributes = configuration.parseAttributesFromMessage(m, folder);
          }
          retrievedEmails.add(Result.<IncomingEmail, T>builder().output(email).attributes(attributes).build());
        }
        deleteAfterReadCallback.accept(connection, attributes);
      }
      return retrievedEmails;
    } catch (MessagingException me) {
      throw new EmailException("Error while retrieving emails: " + me.getMessage(), me);
    }
  }

  @Override
  public List<Result<IncomingEmail, T>> getPage(MailboxConnection connection) {
    this.connection = connection;
    Folder folder = connection.getFolder(folderName);
    int count = getMessageCount(folder);
    if (count != 0) {
      top = min(top, count);
      while (bottom <= top) {
        List<Result<IncomingEmail, T>> emails = list(bottom, top, folder);
        bottom += pageSize;
        top = min(top + pageSize, count);
        if (!emails.isEmpty()) {
          return emails;
        }
      }
    }
    return emptyList();
  }

  private int getMessageCount(Folder folder) {
    try {
      return folder.getMessageCount();
    } catch (MessagingException e) {
      throw new EmailException("Error getting messages count", e);
    }
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
  public void close() throws IOException {
    connection.forceClose(folderName);
  }

  @Override
  public boolean useStickyConnections() {
    return true;
  }

  private MediaType getMediaType(BaseEmailAttributes attributes) {
    String contentType = attributes.getHeaders().get(CONTENT_TYPE_HEADER);
    return contentType == null ? ANY : MediaType.parse(contentType);
  }
}
