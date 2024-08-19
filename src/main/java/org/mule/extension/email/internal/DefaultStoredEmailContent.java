/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;


/**
 * {@link StoredEmailContent} default implementation.
 *
 * @since 1.1
 */
public class DefaultStoredEmailContent implements StoredEmailContent {

  /**
   * The body of the email.
   */
  @Parameter
  private final TypedValue<String> body;

  /**
   * The attachments of the email
   */
  @Parameter
  private final Map<String, TypedValue<InputStream>> attachments;

  /**
   * Creates an instance with the message body and their attachments
   */
  DefaultStoredEmailContent(TypedValue<String> body, Map<String, TypedValue<InputStream>> attachments) {
    this.body = body;
    this.attachments = attachments != null ? new LinkedHashMap<>(attachments) : emptyMap();
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

  @Override
  public String toString() {
    String attachmentNames = attachments.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(", "));
    return format("{\n \"body\": \"%s\", \n \"attachments\": [%s]\n}", body.getValue(), attachmentNames);
  }
}
