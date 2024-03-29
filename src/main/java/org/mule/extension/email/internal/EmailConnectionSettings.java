/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.security.Username;

/**
 * A simple POJO with a basic set of parameters required by every email connection.
 *
 * @since 1.0
 */
public abstract class EmailConnectionSettings {

  /**
   * Host name of the mail server.
   */
  @Parameter
  @Placement(order = 1)
  @Host
  protected String host;

  /**
   * Username used to connect with the mail server.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  @Username
  protected String user;

  /**
   * Username password to connect with the mail server.
   */
  @Parameter
  @Password
  @Optional
  @Placement(order = 4)
  protected String password;

  /**
   * @return the host name of the mail server.
   */
  public String getHost() {
    return host;
  }

  /**
   * @return the username used to connect with the mail server.
   */
  public String getUser() {
    return user;
  }

  /**
   * @return the password corresponding to the {@code username}
   */
  public String getPassword() {
    return password;
  }

  /**
   * @return The connection port
   */
  public abstract String getPort();
}
