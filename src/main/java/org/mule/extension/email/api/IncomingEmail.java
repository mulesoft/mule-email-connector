/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.api;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.metadata.DataType.ATOM_STRING;

import org.mule.runtime.api.metadata.TypedValue;
import java.io.InputStream;
import java.util.List;

/**
 * A received email that lives in a mailbox.
 *
 * @since 1.0
 */
public class IncomingEmail {

  public static final IncomingEmail EMPTY = new IncomingEmail(new TypedValue<>("", ATOM_STRING), emptyList());

  private TypedValue<String> body;
  private List<TypedValue<InputStream>> attachments;

  public IncomingEmail(TypedValue<String> body, List<TypedValue<InputStream>> attachments) {
    this.body = body;
    this.attachments = attachments;
  }

  public List<TypedValue<InputStream>> getAttachments() {
    return attachments;
  }

  public TypedValue<String> getBody() {
    return body;
  }
}
