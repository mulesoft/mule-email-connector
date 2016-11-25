/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static org.mule.extension.email.internal.EmailConnector.TLS_CONFIGURATION;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.IMAPS_PORT;
import org.mule.extension.email.internal.EmailConnectionSettings;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Groups IMAPS connection parameters
 *
 * @since 4.0
 */
public final class IMAPSConnectionSettings extends EmailConnectionSettings {

  /**
   * The port number of the mail server. '993' by default.
   */
  @Parameter
  @Optional(defaultValue = IMAPS_PORT)
  @Placement(order = 2)
  @Summary("The port number of the mail server. '993' by default.")
  private String port;

  /**
   * A factory for TLS contexts. A TLS context is configured with a key store and a trust store. Allows to create a TLS secured
   * connections.
   */
  @Parameter
  @Summary("TLS Configuration for the secure connection of the IMAPS protocol")
  @Placement(order = 5)
  @DisplayName(TLS_CONFIGURATION)
  private TlsContextFactory tlsContextFactory;

  @Override
  public String getPort() {
    return port;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }
}
