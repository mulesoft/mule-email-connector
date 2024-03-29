/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;

import java.util.Map;

/**
 * Generic contract for all email configurations.
 *
 * @since 1.0
 */
public abstract class AbstractEmailConnectionProvider<T> implements PoolingConnectionProvider<T>, NoConnectivityTest {

  private static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";

  @ParameterGroup(name = TIMEOUT_CONFIGURATION)
  private TimeoutSettings timeoutSettings;

  /**
   * An additional custom set of properties to configure the connection session.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 5)
  @ExcludeFromConnectivitySchema
  private Map<String, String> properties;

  /**
   * @return the additional custom properties to configure the session.
   */
  protected Map<String, String> getProperties() {
    return properties;
  }

  /**
   * @return the configured client socket connection timeout in milliseconds.
   */
  protected long getConnectionTimeout() {
    return timeoutSettings.getTimeoutUnit().toMillis(timeoutSettings.getConnectionTimeout());
  }

  /**
   * @return he configured client socket read timeout in milliseconds.
   */
  protected long getReadTimeout() {
    return timeoutSettings.getTimeoutUnit().toMillis(timeoutSettings.getReadTimeout());
  }

  /**
   * @return he configured client socket write timeout in milliseconds.
   */
  protected long getWriteTimeout() {
    return timeoutSettings.getTimeoutUnit().toMillis(timeoutSettings.getWriteTimeout());
  }
}
