/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Configuration for operations that are performed through the SMTP (Simple Mail Transfer Protocol) protocol.
 *
 * @since 1.0
 */
@Operations(SendOperation.class)
@ConnectionProviders({SMTPProvider.class, SMTPSProvider.class})
@Configuration(name = "smtp")
@DisplayName("SMTP")
public class SMTPConfiguration {

  @DefaultEncoding
  private String muleEncoding;
  /**
   * The "From" sender address. The person that is going to send the messages.
   */
  @Parameter
  @Optional
  private String from;

  /**
   * Default character encoding to be used in all the messages. If not specified, the default charset in the mule configuration
   * will be used
   */
  @Parameter
  @Optional
  private String defaultEncoding;

  @Parameter
  @Optional
  private String defaultContentTransferEncoding;

  /**
   * @return the address of the person that is going to send the messages.
   */
  public String getFrom() {
    return from;
  }

  public String getDefaultEncoding() {
    return defaultEncoding == null ? muleEncoding : defaultEncoding;
  }

  public String getDefaultContentTransferEncoding() {
    return defaultContentTransferEncoding;
  }

  public void setDefaultContentTransferEncoding(String defaultContentTransferEncoding) {
    this.defaultContentTransferEncoding = defaultContentTransferEncoding;
  }
}
