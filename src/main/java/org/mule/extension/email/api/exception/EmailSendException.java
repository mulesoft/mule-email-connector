/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.internal.errors.EmailError.SEND;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Email to communicate that an exception occurred trying to send an Email
 *
 * @since 1.0
 */
public class EmailSendException extends ModuleException {

  public EmailSendException(String message, Throwable cause) {
    super(message, SEND, cause);
  }
}
