/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * A simple {@link DataSource} implementation that carries the {@link InputStream} of an email attachment with its content type.
 * 
 * @since 1.0
 */
class EmailAttachmentDataSource implements DataSource {

  private final String name;
  private final InputStream content;
  private final String contentType;

  EmailAttachmentDataSource(String name, InputStream content, String contentType) {
    this.name = name;
    this.content = content;
    this.contentType = contentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return content;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException(EmailAttachmentDataSource.class.getName() + " does not provide an OutputStream");
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String getName() {
    return name;
  }
}
