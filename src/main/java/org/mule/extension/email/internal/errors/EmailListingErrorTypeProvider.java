/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.errors;

import static org.mule.extension.email.internal.errors.EmailError.CONNECTIVITY;
import static org.mule.extension.email.internal.errors.EmailError.EMAIL_LIST;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class EmailListingErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(CONNECTIVITY)
        .add(EMAIL_LIST)
        .build();
  }
}
