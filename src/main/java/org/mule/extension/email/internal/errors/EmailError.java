/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.errors;

import org.mule.extension.email.internal.EmailConnector;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * Errors for the {@link EmailConnector}
 *
 * @since 1.0
 */
public enum EmailError implements ErrorTypeDefinition<EmailError> {

  /**
   * Error while parsing attributes.
   */
  FETCHING_ATTRIBUTES,

  /**
   * Error while marking email flags.
   */
  MARK,

  /**
   * Error while accessing folder.
   */
  ACCESSING_FOLDER(MARK),

  /**
   * Error while deleting emails from folder.
   */
  EXPUNGE_ERROR,

  /**
   * Error while looking for email
   */
  EMAIL_NOT_FOUND(MARK),

  /**
   * Error while sending email
   */
  SEND,

  /**
   * Error while sending attachment
   */
  ATTACHMENT(SEND),

  /**
   * Error listing emails
   */
  EMAIL_LIST,

  /**
   * Error reading email content
   */
  READ_EMAIL,

  /**
   * Error moving an email from one folder to another
   */
  EMAIL_MOVE,

  /**
   * Error getting the count of emails
   */
  EMAIL_COUNT_MESSAGES,

  // Connection related errors

  CONNECTIVITY(MuleErrors.CONNECTIVITY),

  AUTHENTICATION(CONNECTIVITY),

  INVALID_CREDENTIALS(CONNECTIVITY),

  UNKNOWN_HOST(CONNECTIVITY),

  CONNECTION_TIMEOUT(CONNECTIVITY),

  DISCONNECTED(CONNECTIVITY),

  SSL_ERROR(CONNECTIVITY);

  private ErrorTypeDefinition<? extends Enum<?>> error;

  EmailError(ErrorTypeDefinition<? extends Enum<?>> error) {
    this.error = error;
  }

  EmailError() {

  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(error);
  }
}
