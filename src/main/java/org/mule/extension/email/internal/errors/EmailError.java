/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
  FETCHING_ATTRIBUTES,

  FETCHING_EMAILS,

  MARK,

  ACCESSING_FOLDER(MARK),

  EXPUNGE_ERROR,

  EMAIL_NOT_FOUND(MARK),

  SEND,

  ATTACHMENT(SEND),

  EMAIL_LIST,

  CONNECTIVITY(MuleErrors.CONNECTIVITY),

  AUTHENTICATION(CONNECTIVITY),

  INVALID_CREDENTIALS(CONNECTIVITY),

  UNKNOWN_HOST(CONNECTIVITY),

  CONNECTION_TIMEOUT(CONNECTIVITY),

  DISCONNECTED(CONNECTIVITY),

  READ_EMAIL,

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
