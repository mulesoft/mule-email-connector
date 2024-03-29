/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.internal.errors.EmailError.FETCHING_ATTRIBUTES;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.runtime.extension.api.exception.ModuleException;

import javax.mail.Message;

/**
 * {@link ModuleException} for the cases in which there is an error constructing a {@link BaseEmailAttributes} from an email
 * {@link Message}.
 * 
 * @since 1.0
 */
public class CannotFetchMetadataException extends ModuleException {

  public CannotFetchMetadataException(String message, Exception exception) {
    super(message, FETCHING_ATTRIBUTES, exception);
  }
}
