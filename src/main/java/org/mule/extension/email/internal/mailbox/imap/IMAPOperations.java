/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Folder.READ_WRITE;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfigOverrides;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONFIG_OVERRIDES_PARAM_GROUP;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.INBOX_FOLDER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.PAGE_SIZE_ERROR_MESSAGE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.UNLIMITED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.exception.EmailAccessingFolderErrorTypeProvider;
import org.mule.extension.email.api.exception.EmailMarkingErrorTypeProvider;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.commands.EmailSetFlagException;
import org.mule.extension.email.internal.commands.ExpungeCommand;
import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.errors.EmailListingErrorTypeProvider;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.internal.resolver.IMAPArrayStoredEmailContentTypeResolver;
import org.mule.extension.email.internal.value.MailboxFolderValueProvider;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

/**
 * Basic set of operations which perform on top the IMAP email protocol.
 *
 * @since 1.0
 */
public class IMAPOperations {

  private final ExpungeCommand expungeCommand = new ExpungeCommand();
  private final SetFlagCommand setFlagCommand = new SetFlagCommand();

  /**
   * List all the emails (with pagination) in the configured imap mailBoxFolder that match with the specified {@code imapMatcher}
   * criteria.
   *
   * @param config              The {@link MailboxAccessConfiguration} associated to this operation.
   * @param mailboxFolder       Mailbox folder where the emails are going to be fetched
   * @param imapMatcher         Email Matcher which gives the capability of filter the retrieved emails
   * @param deleteAfterRetrieve Specifies if the returned emails must be deleted after being retrieved or not.
   * @param pageSize            Size of the page used by the {@link PagingProvider} implementation for fetching the emails from the IMAP server
   * @param limit               Maximum amount of emails retrieved by the operation. Take into account that this limit only applies to the emails effectively
   *                            retrieved by the operation (the ones which matched the {@link IMAPEmailPredicateBuilder} criteria) and doesn't
   *                            imply any restriction over the amount of emails being retrieved from the mailbox server.
   * @return an {@link PagingProvider} which provides {@link Result}s composed by the email's body and its corresponding {@link IMAPEmailAttributes}.
   */
  @Summary("Lists the emails in the given IMAP Mailbox Folder")
  @DisplayName("List - IMAP")
  @Throws({EmailListingErrorTypeProvider.class, EmailAccessingFolderErrorTypeProvider.class})
  @OutputResolver(output = IMAPArrayStoredEmailContentTypeResolver.class)
  public PagingProvider<MailboxConnection, Result<StoredEmailContent, IMAPEmailAttributes>> listImap(@Config IMAPConfiguration config,
                                                                                                     @Optional(
                                                                                                         defaultValue = INBOX_FOLDER) @OfValues(MailboxFolderValueProvider.class) String mailboxFolder,
                                                                                                     @DisplayName("Match with") @Optional IMAPEmailPredicateBuilder imapMatcher,
                                                                                                     @Optional(
                                                                                                         defaultValue = "false") boolean deleteAfterRetrieve,
                                                                                                     @Optional(
                                                                                                         defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                                                     @Optional(
                                                                                                         defaultValue = UNLIMITED) int limit,
                                                                                                     StreamingHelper streamingHelper,
                                                                                                     @ParameterGroup(
                                                                                                         name = CONFIG_OVERRIDES_PARAM_GROUP) MailboxAccessConfigOverrides overrides) {
    checkArgument(pageSize > 0, format(PAGE_SIZE_ERROR_MESSAGE, pageSize));
    return new PagingProviderEmailDelegate<>(config, mailboxFolder, imapMatcher, pageSize, limit, deleteAfterRetrieve,
                                             (connection, attributes) -> setFlagCommand.setByUID(connection, mailboxFolder,
                                                                                                 DELETED,
                                                                                                 parseLong(attributes.getId())),
                                             streamingHelper, overrides);
  }

  /**
   * Marks an incoming email as DELETED, this way the marked email(s) are scheduled for deletion when the folder closes, this
   * means that the email is not physically eliminated from the mailbox folder, but it's state changes.
   * <p>
   * All DELETED marked emails are going to be eliminated from the mailbox when one of
   * {@link IMAPOperations#expungeFolder(MailboxConnection, String)} or
   * {@link IMAPOperations#delete(MailboxConnection, String, long)} is executed.
   * <p>
   * This operation targets a single email.
   *
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be marked as deleted
   * @param emailId       Email ID Number of the email to mark as deleted.
   */
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void markAsDeleted(@Connection MailboxConnection connection,
                            @Optional(
                                defaultValue = INBOX_FOLDER) @OfValues(MailboxFolderValueProvider.class) String mailboxFolder,
                            @Summary("Email ID Number of the email to mark as deleted") @DisplayName("Email ID") long emailId) {
    setFlagCommand.setByUID(connection, mailboxFolder, DELETED, emailId);
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
                         @Optional(defaultValue = INBOX_FOLDER) @OfValues(MailboxFolderValueProvider.class) String mailboxFolder,
                         @Summary("Email ID Number of the email to mark as read") @DisplayName("Email ID") long emailId) {
    setFlagCommand.setByUID(connection, mailboxFolder, SEEN, emailId);
  }

  /**
   * Eliminates from the mailbox all the messages scheduled for deletion with the DELETED flag set.
   *
   * @param connection    The associated {@link MailboxConnection}.
   * @param mailboxFolder Mailbox folder where the emails with the 'DELETED' flag are going to be scheduled to be definitely
   *                      deleted
   */

  @Throws(EmailAccessingFolderErrorTypeProvider.class)
  public void expungeFolder(@Connection MailboxConnection connection,
                            @Optional(
                                defaultValue = INBOX_FOLDER) @OfValues(MailboxFolderValueProvider.class) String mailboxFolder) {
    expungeCommand.expunge(connection, mailboxFolder);
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
                     @Optional(defaultValue = INBOX_FOLDER) @OfValues(MailboxFolderValueProvider.class) String mailboxFolder,
                     @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") long emailId) {
    markAsDeleted(connection, mailboxFolder, emailId);
    try {
      connection.getFolder(mailboxFolder, READ_WRITE).close(true);
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw new EmailSetFlagException(format("Error while eliminating email uid:[%s] from the [%s] folder", emailId,
                                             mailboxFolder),
                                      e);
    }
  }
}
