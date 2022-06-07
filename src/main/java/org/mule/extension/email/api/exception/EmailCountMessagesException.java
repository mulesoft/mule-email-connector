/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.internal.errors.EmailError.EMAIL_COUNT_MESSAGES;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} for the cases in which there was a problem counting messages in the specified folder
 *
 * @since 1.7
 */
public class EmailCountMessagesException extends ModuleException {

  public EmailCountMessagesException(String message, Exception exception) {
    super(message, EMAIL_COUNT_MESSAGES, exception);
  }

}
