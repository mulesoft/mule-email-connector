/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import org.mule.extension.email.internal.AbstractEmailConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

/**
 * Generic contract for all email retriever {@link ConfigurationProvider}s.
 *
 * @since 1.0
 */
public abstract class AbstractMailboxConnectionProvider extends AbstractEmailConnectionProvider<MailboxConnection> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(MailboxConnection connection) {
    connection.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(MailboxConnection connection) {
    return connection.validate();
  }

  @Override
  public void onReturn(MailboxConnection connection) {
    // If the folder remains open after an operation execution failed.
    connection.closeFolder(false);
  }
}
