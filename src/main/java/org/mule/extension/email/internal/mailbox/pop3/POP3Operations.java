/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONFIG_OVERRIDES_PARAM_GROUP;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGINATION_OFFSET;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.INBOX_FOLDER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.PAGE_SIZE_ERROR_MESSAGE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.PAGINATION_OFFSET_ERROR_MESSAGE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.UNLIMITED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static java.lang.String.format;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Folder.READ_ONLY;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.POP3EmailAttributes;
import org.mule.extension.email.api.exception.EmailAccessingFolderException;
import org.mule.extension.email.api.exception.EmailCountMessagesException;
import org.mule.extension.email.api.exception.EmailCountingErrorTypeProvider;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.api.predicate.POP3EmailPredicateBuilder;
import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.errors.EmailListingErrorTypeProvider;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfigOverrides;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.internal.resolver.POP3ArrayStoredEmailContentTypeResolver;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import javax.mail.MessagingException;

import com.sun.mail.pop3.POP3Folder;

/**
 * A set of operations which perform on top the POP3 email protocol.
 *
 * @since 1.0
 */
public class POP3Operations {

  private final SetFlagCommand setFlagCommand = new SetFlagCommand();

  /**
   * List all the emails (with pagination) in the configured pop3 mailBoxFolder that match with the specified {@code pop3Matcher}
   * criteria.
   * <p>
   * As the POP3 protocol does not support the capability to find specific emails from its UID in a folder to move/delete it. a
   * parameter {@code deleteAfterRetrieve} is available for deleting the emails from the server right after being retrieved.
   *
   * @param config              The {@link MailboxAccessConfiguration} associated to this operation.
   * @param mailboxFolder       Mailbox folder where the emails are going to be fetched
   * @param pop3Matcher         Email Matcher which gives the capability of filter the retrieved emails
   * @param deleteAfterRetrieve Specifies if the returned emails must be deleted after being retrieved or not.
   * @param pageSize            Size of the page used by the {@link PagingProvider} implementation for fetching the emails from the POP3 server
   * @param paginationOffset    Size of the pagination offset used by the {@link PagingProvider} implementation for fetching the emails from the POP3 server
   * @param limit               Maximum amount of emails retrieved by the operation. Take into account that this limit only applies to the emails effectively
   *                            retrieved by the operation (the ones which matched the {@link IMAPEmailPredicateBuilder} criteria) and doesn't
   *                            imply any restriction over the amount of emails being retrieved from the mailbox server.
   * @return an {@link PagingProvider} which provides {@link Result}s composed by the email's body and its corresponding {@link POP3EmailAttributes}.
   */
  @Summary("Lists the emails in the given POP3 Mailbox Folder")
  @DisplayName("List - POP3")
  @Throws(EmailListingErrorTypeProvider.class)
  @OutputResolver(output = POP3ArrayStoredEmailContentTypeResolver.class)
  public PagingProvider<MailboxConnection, Result<StoredEmailContent, POP3EmailAttributes>> listPop3(@Config POP3Configuration config,
                                                                                                     @Optional(
                                                                                                         defaultValue = INBOX_FOLDER) String mailboxFolder,
                                                                                                     @DisplayName("Match with") @Optional POP3EmailPredicateBuilder pop3Matcher,
                                                                                                     @Optional(
                                                                                                         defaultValue = "false") boolean deleteAfterRetrieve,
                                                                                                     @Optional(
                                                                                                         defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                                                     @Optional(
                                                                                                         defaultValue = DEFAULT_PAGINATION_OFFSET) int paginationOffset,
                                                                                                     @Optional(
                                                                                                         defaultValue = UNLIMITED) int limit,
                                                                                                     StreamingHelper streamingHelper,
                                                                                                     @ParameterGroup(
                                                                                                         name = CONFIG_OVERRIDES_PARAM_GROUP) MailboxAccessConfigOverrides overrides) {
    checkArgument(pageSize > 0, format(PAGE_SIZE_ERROR_MESSAGE, pageSize));
    checkArgument(paginationOffset >= 0, format(PAGINATION_OFFSET_ERROR_MESSAGE, pageSize));
    return new PagingProviderEmailDelegate<>(config, mailboxFolder, pop3Matcher, pageSize, paginationOffset, limit,
                                             deleteAfterRetrieve,
                                             (connection, attributes) -> setFlagCommand.setByNumber(connection, mailboxFolder,
                                                                                                    DELETED,
                                                                                                    attributes.getNumber()),
                                             streamingHelper, overrides);
  }

  /**
   * Counts the emails in the {@code INBOX_FOLDER}.
   * <p>
   *
   * @param connection          The corresponding {@link MailboxConnection} instance.
   */
  @Summary("Get the total amount of messages")
  @DisplayName("Count messages - POP3")
  @Throws(EmailCountingErrorTypeProvider.class)
  public int countMessagesPop3(@Connection MailboxConnection connection) {
    try {
      POP3Folder folder = (POP3Folder) connection.getFolder(INBOX_FOLDER, READ_ONLY);

      int count = folder.getMessageCount();
      folder.close();
      return count;

    } catch (EmailAccessingFolderException | MessagingException e) {
      throw new EmailCountMessagesException("Error while counting messages in the INBOX folder", e);
    }
  }
}
