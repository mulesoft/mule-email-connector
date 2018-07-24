/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static org.mule.extension.email.api.EmailError.MARK;

import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Exception to communicate that an exception occurred trying to set a flag in a Email
 *
 * @since 1.0
 */
public class EmailSetFlagException extends ModuleException {

  public EmailSetFlagException(String message, Throwable cause) {
    super(message, MARK, cause);
  }
}
