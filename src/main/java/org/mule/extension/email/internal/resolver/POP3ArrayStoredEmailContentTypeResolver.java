/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.resolver;


import org.mule.extension.email.api.attributes.POP3EmailAttributes;

/**
 * A {@link ArrayStoredEmailContentTypeResolver} for the POP list operation
 *
 * @since 1.1.3
 */
public class POP3ArrayStoredEmailContentTypeResolver extends ArrayStoredEmailContentTypeResolver {

  public POP3ArrayStoredEmailContentTypeResolver() {
    super(POP3EmailAttributes.class);
  }

  @Override
  public String getResolverName() {
    return this.getClass().getSimpleName();
  }

}
