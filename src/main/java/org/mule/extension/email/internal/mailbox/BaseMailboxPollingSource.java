/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import com.sun.mail.pop3.POP3Folder;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.ExpungeFolderException;
import org.mule.extension.email.api.exception.EmailListException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.internal.DefaultStoredEmailContent;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import java.util.function.Predicate;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.errors.EmailError.*;

/**
 * Implementation of an email pooling source, contains all the logic to retrieve and push emails, add watermark and delete
 * retrieved emails if required.
 * <p>
 * This class should be implemented by protocols that can retrieve mails specifying if they can handle watermark or not,
 * and what kind of filter can be applied for the retrieved emails.
 *
 * @since 1.1
 */
public abstract class BaseMailboxPollingSource extends PollingSource<DefaultStoredEmailContent, BaseEmailAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseMailboxPollingSource.class);

  @Config
  private MailboxAccessConfiguration config;

  @Connection
  private ConnectionProvider<MailboxConnection> connectionProvider;

  private MailboxConnection connection;

  private Predicate<BaseEmailAttributes> predicate;

  /**
   * The name of the folder to poll emails from. Defaults to "INBOX".
   */
  @Parameter
  @Optional(defaultValue = "INBOX")
  private String folder;

  /**
   * Enables the deletion of the polled emails after being retrieved. This is disabled by default.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean deleteAfterRetrieve;

  private StoredEmailContentFactory storedEmailContentFactory;

  private Folder openFolder;

  /**
   * @return an instance of {@link BaseEmailPredicateBuilder} used to filter the retrieved emails.
   */
  protected abstract java.util.Optional<? extends BaseEmailPredicateBuilder> getPredicateBuilder();

  /**
   * @return true if watermark should be added to the polled items, false if not.
   */
  protected abstract boolean isWatermarkEnabled();

  /**
   * Starts the flow by setting up all the required objects for polling such as the matcher, email parser and connection.
   *
   * @throws ConnectionException if a problem occur while obtaining the connection.
   */
  @Override
  protected void doStart() throws ConnectionException {
    java.util.Optional<? extends BaseEmailPredicateBuilder> builder = getPredicateBuilder();
    predicate = builder.isPresent() ? builder.get().build() : a -> true;
    storedEmailContentFactory = new StoredEmailContentFactory();
    connection = connectionProvider.connect();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Closes the provided connection.
   */
  @Override
  protected void doStop() {
    LOGGER.debug("Stopping Email Listener");

    if (connection != null) {
      connectionProvider.disconnect(connection);
    }
  }

  /**
   * Polls emails from the configured folder.
   * <p>
   * This method handles the deletion of the retrieved emails if specified and sets watermark to all the polled items if required.
   * <p>
   * The folder is opened at the end of the method to get an updated state of the folder and is closed at the end to save the
   * state of the mailbox with changes such as flag marks and deletion of the polled elements.
   * <p>
   * All retrieved emails are parsed to extract its attributes and content and are ALWAYS opened.
   */
  @Override
  public void poll(PollContext<DefaultStoredEmailContent, BaseEmailAttributes> pollContext) {
    openFolder = connection.getFolder(folder, READ_WRITE);
    for (Message message : getMessages(openFolder)) {
      BaseEmailAttributes attributes = config.parseAttributesFromMessage(message, openFolder);
      String id = attributes.getId();
      if (predicate.test(attributes)) {
        pollContext.accept(item -> {
          if (isWatermarkEnabled()) {
            item.setWatermark(Long.valueOf(id));
          }
          item.setId(id);
          item.setResult(Result.<DefaultStoredEmailContent, BaseEmailAttributes>builder()
              .output(getEmailContent(message, id))
              .attributes(attributes)
              .build());

          if (deleteAfterRetrieve) {
            markAsDeleted(id);
          }
        });
      }
    }
    connection.closeFolder(deleteAfterRetrieve);
  }

  /**
   * Logs a warning if an email was rejected for processing.
   */
  @Override
  public void onRejectedItem(Result<DefaultStoredEmailContent, BaseEmailAttributes> result,
                             SourceCallbackContext sourceCallbackContext) {
    result.getAttributes().ifPresent(a -> LOGGER.warn("Email [" + a.getId() + "] was not processed."));
  }

  private Message[] getMessages(Folder openFolder) {
    try {
      return openFolder.getMessages();
    } catch (MessagingException e) {
      throw new EmailListException("Error retrieving emails: " + e.getMessage(), e);
    }
  }

  /**
   * Marks an email as deleted looking it by its UID.
   */
  private void markAsDeleted(String id) {
    try {
      if (openFolder instanceof POP3Folder) {
        for (Message message : openFolder.getMessages()) {
          if (((POP3Folder) openFolder).getUID(message).equals(id)) {
            message.setFlag(DELETED, true);
          }
        }
      } else if (openFolder instanceof UIDFolder) {
        Message message = ((UIDFolder) openFolder).getMessageByUID(Long.valueOf(id));
        message.setFlag(DELETED, true);
      }
    } catch (MessagingException e) {
      throw new ExpungeFolderException("Error while setting delete flag on email uid [" + id + "]", e);
    }
  }

  private DefaultStoredEmailContent getEmailContent(Message message, String id) {
    try {
      return (DefaultStoredEmailContent) storedEmailContentFactory.fromMessage(message);
    } catch (Exception e) {
      throw new ModuleException("Error reading email: [" + id + "]:" + e.getMessage(), READ_EMAIL, e);
    }
  }
}
