/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.internal.errors.EmailError.ACCESSING_FOLDER;
import static org.mule.extension.email.internal.errors.EmailError.EMAIL_NOT_FOUND;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Errors that can be thrown when setting a flag on an email
 * 
 * @since 1.0
 */
public class EmailMarkingErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(EMAIL_NOT_FOUND)
        .add(ACCESSING_FOLDER)
        .build();
  }
}

