/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static org.mule.extension.email.internal.util.EmailConnectorConstants.SMTPS_PORT;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.email.internal.AbstractEmailConnection;
import org.mule.extension.email.internal.EmailProtocol;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.slf4j.Logger;

import java.util.Map;

/**
 * A connection with a mail server for sending emails.
 *
 * @since 1.0
 */
public final class SenderConnection extends AbstractEmailConnection {

  private static Logger LOGGER = getLogger(SenderConnection.class);

  /**
   * Creates a new instance.
   *
   * @param protocol the protocol used to send mails. Smtp or smtps
   * @param username the username to establish connection with the mail server.
   * @param password the password corresponding to the {@code username}
   * @param host the host name of the mail server.
   * @param port the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout the socket read timeout
   * @param writeTimeout the socket write timeout
   * @param properties the custom properties added to configure the session.
   */
  public SenderConnection(EmailProtocol protocol, String username, String password, String host, String port,
                          long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties)
      throws EmailConnectionException {
    super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
  }

  /**
   * Creates a new instance of the connection secured by TLS.
   *
   * @param protocol the protocol used to send mails. Smtp or smtps
   * @param username the username to establish connection with the mail server.
   * @param password the password corresponding to the {@code username}
   * @param host the host name of the mail server.
   * @param port the port number of the mail server.
   * @param connectionTimeout the socket connection timeout
   * @param readTimeout the socket read timeout
   * @param writeTimeout the socket write timeout
   * @param properties the custom properties added to configure the session.
   * @param tlsContextFactory the tls context factory for creating the context to secure the connection
   */
  public SenderConnection(EmailProtocol protocol, String username, String password, String host, String port,
                          long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties,
                          TlsContextFactory tlsContextFactory)
      throws EmailConnectionException {
    super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, tlsContextFactory);
    warnPossibleGmailConflict(host, port);
  }

  private void warnPossibleGmailConflict(String host, String port) {
    if (host.contains("gmail") && port.equals(SMTPS_PORT)) {
      LOGGER.warn("Connecting with GMail through SSL port [" + SMTPS_PORT + "]. "
          + "This might not work for some authentication mechanisms such as Two-Factor auth secured mailboxes. "
          + "Use port [587] for Gmail TLS secured connections.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect() {
    // No implementation
  }

  /**
   * {@inheritDoc}
   * <p>
   * if the {@link SenderConnection} instance exists, then the validation will always be successful
   */
  @Override
  public ConnectionValidationResult validate() {
    return success();
  }
}
