/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static java.util.Optional.ofNullable;

import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.api.predicate.POP3EmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.BaseMailboxPollingSource;
import org.mule.extension.email.internal.resolver.StoredEmailContentTypeResolver;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * Retrieves all the emails from an POP3 mailbox folder.
 *
 * @since 1.1
 */
@DisplayName("On New Email - POP3")
@Alias("listener-pop3")
@MetadataScope(outputResolver = StoredEmailContentTypeResolver.class)
public class POP3PollingSource extends BaseMailboxPollingSource {

  /**
   * A matcher to filter emails retrieved by this polling source.
   */
  @Parameter
  @Optional
  private POP3EmailPredicateBuilder pop3Matcher;

  /**
   * {@inheritDoc}
   */
  @Override
  protected java.util.Optional<? extends BaseEmailPredicateBuilder> getPredicateBuilder() {
    return ofNullable(pop3Matcher);
  }

  /**
   * {@inheritDoc}
   * <p>
   * POP3 does not support watermarking.
   *
   * @return false.
   */
  @Override
  protected boolean isWatermarkEnabled() {
    return false;
  }
}
