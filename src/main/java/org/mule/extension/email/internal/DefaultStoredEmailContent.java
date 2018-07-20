/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.InputStream;
import java.util.Map;


/**
 * {@link StoredEmailContent} default implementation.
 *
 * @since 1.1
 */
public class DefaultStoredEmailContent implements StoredEmailContent {

  private final TypedValue<String> body;
  private final Map<String, TypedValue<InputStream>> attachments;

  /**
   * Creates an instance with the message body and their attachments
   */
  DefaultStoredEmailContent(TypedValue<String> body, Map<String, TypedValue<InputStream>> attachments) {
    this.body = body;
    this.attachments = attachments;
  }

  /**
   * {@inheritDoc}
   */
  public TypedValue<String> getBody() {
    return body;
  }

  /**
   * {@inheritDoc}
   */
  public Map<String, TypedValue<InputStream>> getAttachments() {
    return attachments;
  }
}
