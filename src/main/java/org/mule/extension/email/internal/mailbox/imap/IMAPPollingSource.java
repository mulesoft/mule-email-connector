/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.BaseMailboxPollingSource;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import static java.util.Optional.*;

/**
 * Retrieves all the emails from an IMAP mailbox folder, watermark can be enabled for polled items.
 *
 * @since 1.1
 */
@DisplayName("On New Email - IMAP")
@Alias("listener-imap")
public class IMAPPollingSource extends BaseMailboxPollingSource {

  /**
   * If watermark should be applied to the polled emails or not. Default to true.
   */
  @Parameter
  @DisplayName("Enable Watermark")
  @Optional(defaultValue = "true")
  private boolean watermarkEnabled;

  /**
   * A matcher to filter emails retrieved by this polling source. For default already read emails will be filtered.
   */
  @Parameter
  @Optional
  private IMAPEmailPredicateBuilder imapMatcher;

  /**
   * {@inheritDoc}
   */
  @Override
  protected java.util.Optional<? extends BaseEmailPredicateBuilder> getPredicateBuilder() {
    return of(imapMatcher == null ? new DefaultPollingSourceMatcher() : imapMatcher);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isWatermarkEnabled() {
    return watermarkEnabled;
  }
}
