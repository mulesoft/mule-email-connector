/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.api.EmailError.EMAIL_LIST;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Exception to communicate that an error occurred trying to list emails.
 *
 * @since 1.0
 */
public class EmailListException extends ModuleException {

  public EmailListException(String message, Throwable cause) {
    super(message, EMAIL_LIST, cause);
  }
}
