/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import org.mule.runtime.extension.api.exception.ModuleException;

import static org.mule.extension.email.internal.errors.EmailError.EXPUNGE_ERROR;

/**
 * {@link ModuleException} for the cases in which there was a problem deleting emails from a folder.
 * 
 * @since 1.1
 */
public class ExpungeFolderException extends ModuleException {

  public ExpungeFolderException(String message, Exception exception) {
    super(message, EXPUNGE_ERROR, exception);
  }
}
