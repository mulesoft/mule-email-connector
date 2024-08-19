/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.internal.errors.EmailError.ACCESSING_FOLDER;
import static org.mule.extension.email.internal.errors.EmailError.EMAIL_NOT_FOUND;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Errors that can be thrown when setting a flag on an email
 * 
 * @since 1.0
 */
public class EmailMarkingErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Collections.unmodifiableSet(new HashSet<ErrorTypeDefinition>(Arrays.asList(EMAIL_NOT_FOUND, ACCESSING_FOLDER)));
  }
}

