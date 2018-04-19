/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.errors;

import static org.mule.extension.email.internal.errors.EmailError.CONNECTIVITY;
import static org.mule.extension.email.internal.errors.EmailError.SEND;

import org.mule.extension.email.internal.sender.SendOperation;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Errors that can be thrown in the {@link SendOperation#send} operation.
 *
 * @since 1.0
 */
public class SendErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Collections.unmodifiableSet(new HashSet<ErrorTypeDefinition>(Arrays.asList(CONNECTIVITY, SEND)));
  }
}

