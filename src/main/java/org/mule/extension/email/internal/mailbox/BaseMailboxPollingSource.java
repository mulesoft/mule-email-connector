/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.errors.EmailError.READ_EMAIL;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONFIG_OVERRIDES_PARAM_GROUP;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailListException;
import org.mule.extension.email.api.exception.ExpungeFolderException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.extension.email.internal.value.MailboxFolderValueProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an email pooling source, contains all the logic to retrieve and push emails, add watermark and delete
 * retrieved emails if required.
 * <p>
 * This class should be implemented by protocols that can retrieve mails specifying if they can handle watermark or not, and what
 * kind of filter can be applied for the retrieved emails.
 *
 * @since 1.1
 */
public abstract class BaseMailboxPollingSource extends PollingSource<StoredEmailContent, BaseEmailAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseMailboxPollingSource.class);

  @Config
  private MailboxAccessConfiguration config;

  @ParameterGroup(name = CONFIG_OVERRIDES_PARAM_GROUP)
  private MailboxAccessConfigOverrides overrides;

  @Connection
  private ConnectionProvider<MailboxConnection> connectionProvider;

  private MailboxConnection connection;

  private Predicate<BaseEmailAttributes> predicate;

  private AtomicInteger usingFolderCounter;

  /**
   * The name of the folder to poll emails from. Defaults to "INBOX".
   */
  @Parameter
  @Optional(defaultValue = "INBOX")
  @OfValues(MailboxFolderValueProvider.class)
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
    usingFolderCounter = new AtomicInteger(0);
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

    connection = null;
    usingFolderCounter = null;
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
   * <p>
   * By default only UNREAD emails are going to be polled.
   */
  @Override
  public void poll(PollContext<StoredEmailContent, BaseEmailAttributes> pollContext) {
    if (isFolderBeingUsed()) {
      LOGGER.debug("Poll will be skipped, since last poll emails are still being processed");
      return;
    }
    try {
      beginUsingFolder();
      for (Message message : getMessages(openFolder)) {
        BaseEmailAttributes attributes = config.parseAttributesFromMessage(message, openFolder);
        String id = attributes.getId();
        if (predicate.test(attributes)) {
          emailDispatchedToFlow();
          pollContext.accept(item -> {
            if (isWatermarkEnabled()) {
              item.setWatermark(Long.valueOf(id));
            }
            item.setId(id);
            item.setResult(Result.<StoredEmailContent, BaseEmailAttributes>builder()
                .output(getEmailContent(message, id))
                .attributes(attributes)
                .build());

            if (deleteAfterRetrieve) {
              markAsDeleted(id, message);
            }
          });
        }
      }
    } finally {
      endUsingFolder();
    }
  }

  private boolean isFolderBeingUsed() {
    synchronized (usingFolderCounter) {
      return usingFolderCounter.get() != 0;
    }
  }

  protected void emailDispatchedToFlow() {
    // Do nothing.
  }

  protected void beginUsingFolder() {
    synchronized (usingFolderCounter) {
      int currentUsingFolderCounter = usingFolderCounter.incrementAndGet();
      if (currentUsingFolderCounter == 1) {
        openFolder = connection.getFolder(folder, READ_WRITE);
      }
    }
  }

  protected void endUsingFolder() {
    synchronized (usingFolderCounter) {
      int currentUsingFolderCounter = usingFolderCounter.decrementAndGet();
      if (currentUsingFolderCounter == 0) {
        connection.closeFolder(deleteAfterRetrieve);
      }
    }
  }

  /**
   * Logs a warning if an email was rejected for processing.
   */
  @Override
  public void onRejectedItem(Result<StoredEmailContent, BaseEmailAttributes> result,
                             SourceCallbackContext sourceCallbackContext) {
    result.getAttributes().ifPresent(a -> LOGGER.debug("Email [" + a.getId() + "] was not processed."));
  }

  protected Message[] getMessages(Folder openFolder) {
    try {
      return openFolder.getMessages();
    } catch (MessagingException e) {
      throw new EmailListException("Error retrieving emails: " + e.getMessage(), e);
    }
  }

  /**
   * Marks an email as deleted looking it by its UID.
   */
  private void markAsDeleted(String id, Message message) {
    try {
      message.setFlag(DELETED, true);
    } catch (MessagingException e) {
      throw new ExpungeFolderException("Error while setting delete flag on email uid [" + id + "]", e);
    }
  }

  private StoredEmailContent getEmailContent(Message message, String id) {
    try {
      return storedEmailContentFactory.fromMessage(message, overrides.getAttachmentNamingStrategy());
    } catch (Exception e) {
      throw new ModuleException("Error reading email: [" + id + "]:" + e.getMessage(), READ_EMAIL, e);
    }
  }
}
