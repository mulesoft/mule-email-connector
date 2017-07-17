/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.lang.String.format;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.INBOX_FOLDER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.PAGE_SIZE_ERROR_MESSAGE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.email.api.EmailMetadataKey;
import org.mule.extension.email.api.EmailMetadataResolver;
import org.mule.extension.email.api.IncomingEmail;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.exception.EmailMarkingErrorTypeProvider;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Basic set of operations which perform on top the IMAP email protocol.
 *
 * @since 1.0
 */
public class IMAPOperations {

  private final SetFlagCommand setFlagCommand = new SetFlagCommand();

  /**
   * List all the emails (with pagination) in the configured imap mailBoxFolder that match with the specified {@code imapMatcher}
   * criteria.
   *
   * @param config              The {@link MailboxAccessConfiguration} associated to this operation.
   * @param mailboxFolder       Mailbox folder where the emails are going to be fetched
   * @param imapMatcher         Email Matcher which gives the capability of filter the retrieved emails
   * @param deleteAfterRetrieve Specifies if the returned emails must be deleted after being retrieved or not.
   * @return an {@link PagingProvider} composed with an {@link Result} with a {@link List} carrying all the emails content and
   * it's corresponding {@link IMAPEmailAttributes}.
   */
  @Summary("List all the emails in the given POP3 Mailbox Folder")
  @DisplayName("List")
  @OutputResolver(output = EmailMetadataResolver.class)
  public PagingProvider<MailboxConnection, Result<IncomingEmail, IMAPEmailAttributes>> listImap(@Config IMAPConfiguration config,
                                                                                                @Optional(
                                                                                                    defaultValue = INBOX_FOLDER) String mailboxFolder,
                                                                                                @DisplayName("Match with") @Optional IMAPEmailPredicateBuilder imapMatcher,
                                                                                                @Optional(
                                                                                                    defaultValue = "false") boolean deleteAfterRetrieve,
                                                                                                @MetadataKeyId @Optional(
                                                                                                    defaultValue = "ANY") @Placement(
                                                                                                        tab = ADVANCED_TAB) EmailMetadataKey outputType,
                                                                                                @Optional(
                                                                                                    defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
    checkArgument(pageSize > 0, format(PAGE_SIZE_ERROR_MESSAGE, pageSize));
    BiConsumer<MailboxConnection, BaseEmailAttributes> delegate = (connection, attributes) -> {
      if (deleteAfterRetrieve) {
        setFlagCommand.setByUID(connection, mailboxFolder, DELETED, attributes.getId());
      }
    };
    return new PagingProviderEmailDelegate<>(config, mailboxFolder, imapMatcher, pageSize, delegate);
  }

  /**
   * Marks a single email as READ changing it's state in the specified mailbox folder.
   * <p>
   * This operation can targets a single email.
   *
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Folder where the emails are going to be marked as read
   * @param emailId       Email ID Number of the email to mark as read.
   */
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void markAsRead(@Connection MailboxConnection connection,
                         @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                         @Summary("Email ID Number of the email to mark as read") @DisplayName("Email ID") long emailId) {
    setFlagCommand.setByUID(connection, mailboxFolder, SEEN, emailId);
  }

  /**
   * Eliminates from the mailbox the email with id {@code emailId}.
   * <p>
   * For IMAP mailboxes all the messages scheduled for deletion (marked as DELETED) will also be erased from the folder.
   *
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be deleted
   * @param emailId       Email ID Number of the email to delete.
   */
  @Summary("Deletes an email from the given Mailbox Folder")
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void delete(@Connection MailboxConnection connection,
                     @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                     @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") long emailId) {
    Folder folder = connection.getFolder(mailboxFolder);
    setFlagCommand.setByUID(connection, mailboxFolder, DELETED, emailId);
    try {
      folder.expunge();
    } catch (MessagingException e) {
      throw new EmailException("Error expunging marked as deleted email", e);
    }
  }
}
