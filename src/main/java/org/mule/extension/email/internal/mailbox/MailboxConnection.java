/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.api.exception.EmailError.ACCESSING_FOLDER;
import static org.mule.extension.email.api.exception.EmailError.DISCONNECTED;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.extension.email.api.exception.EmailAccessingFolderException;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.internal.AbstractEmailConnection;
import org.mule.extension.email.internal.EmailProtocol;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.exception.ModuleException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import java.util.Map;

/**
 * A connection with a mail server for retrieving and managing emails from an specific folder in a mailbox.
 *
 * @since 1.0
 */
public class MailboxConnection extends AbstractEmailConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailboxConnection.class);

  private final LoadingCache<String, Folder> folders;
  private final Store store;

  /**
   * Creates a new instance of the of thee {@link MailboxConnection} secured by TLS.
   *
   * @param protocol          the desired protocol to use. One of imap or pop3 (and its secure versions)
   * @param username          the username to establish the connection.
   * @param password          the password corresponding to the {@code username}.
   * @param host              the host name of the mail server
   * @param port              the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout       the socket read timeout
   * @param writeTimeout      the socket write timeout
   * @param properties        additional custom properties.
   * @param tlsContextFactory the tls context factory for creating the context to secure the connection
   */
  public MailboxConnection(EmailProtocol protocol,
                           String username,
                           String password,
                           String host,
                           String port,
                           long connectionTimeout,
                           long readTimeout,
                           long writeTimeout,
                           Map<String, String> properties,
                           TlsContextFactory tlsContextFactory)
      throws EmailConnectionException {
    super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, tlsContextFactory);
    this.folders = CacheBuilder.newBuilder().expireAfterAccess(1, MINUTES)
        .removalListener(new FolderRemovalListener(true))
        .build(new FolderCacheLoader());
    this.store = MailboxStoreFactory.getStore(session, protocol, username, password);
  }

  /**
   * Creates a new instance of the of the {@link MailboxConnection}.
   *
   * @param protocol          the desired protocol to use. One of imap or pop3 (and its secure versions)
   * @param username          the username to establish the connection.
   * @param password          the password corresponding to the {@code username}.
   * @param host              the host name of the mail server
   * @param port              the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout       the socket read timeout
   * @param writeTimeout      the socket write timeout
   * @param properties        additional custom properties.
   */
  public MailboxConnection(EmailProtocol protocol,
                           String username,
                           String password,
                           String host,
                           String port,
                           long connectionTimeout,
                           long readTimeout,
                           long writeTimeout,
                           Map<String, String> properties)
      throws EmailConnectionException {
    this(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, null);
  }

  public Folder getFolder(String folder) {
    try {
      return folders.get(folder);
    } catch (Exception e) {
      throw new EmailAccessingFolderException(format("Error getting mailbox folder [%s]", folder), e);
    }
  }

  public void forceClose(String folder) {
    try {
      folders.invalidate(folder);
    } catch (Exception e) {
      throw new EmailAccessingFolderException(format("Error closing folder [%s]", folder), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void disconnect() {
    try {
      folders.invalidateAll();
    } catch (Exception e) {
      LOGGER.error(format("Error closing mailbox folders when disconnecting: %s", e.getMessage()));
    } finally {
      try {
        store.close();
      } catch (Exception e) {
        LOGGER.error(format("Error closing store when disconnecting: %s", e.getMessage()));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate() {
    String errorMessage = "Store is not connected";
    return store.isConnected() ? success() : failure(errorMessage, new EmailConnectionException(errorMessage, DISCONNECTED));
  }

  private class FolderRemovalListener implements RemovalListener<String, Folder> {

    private final boolean shouldExpunge;

    FolderRemovalListener(boolean shouldExpunge) {
      this.shouldExpunge = shouldExpunge;
    }

    @Override
    public void onRemoval(RemovalNotification<String, Folder> notification) {
      Folder folder = notification.getValue();
      try {
        if (folder != null && folder.isOpen()) {
          folder.close(shouldExpunge);
        }
      } catch (MessagingException e) {
        throw new ModuleException(ACCESSING_FOLDER, e);
      }
    }
  }


  public class FolderCacheLoader extends CacheLoader<String, Folder> {

    @Override
    public Folder load(String folderName) throws Exception {
      try {
        Folder folder = store.getFolder(folderName);
        folder.open(READ_WRITE);
        return folder;
      } catch (MessagingException e) {
        throw new ModuleException(ACCESSING_FOLDER, e);
      }
    }
  }
}
