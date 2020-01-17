/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;

import java.util.function.Predicate;

/**
 * Default matcher for IMAP Polling source, always check if the email was not read.
 *
 * @since 1.1
 */
public class DefaultPollingSourceMatcher extends IMAPEmailPredicateBuilder {

  /**
   * @return a new Default predicate instance for IMAP
   */
  @Override
  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {
    Predicate<IMAPEmailAttributes> predicate = a -> true;
    predicate.and(a -> !a.getFlags().isSeen());
    return predicate;
  }
}
