/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.api.metadata.TypedValue;

import jakarta.mail.Message;

import java.io.InputStream;
import java.util.Map;


/**
 * Given a {@link Message} that lives in a mailbox introspects it's content to obtain the body an the attachments if any.
 *
 * @since 1.0
 */
public interface StoredEmailContent {

  /**
   * @return the text body of the message.
   */
  TypedValue<String> getBody();

  /**
   * @return a {@link Map} with the attachments of an email bounded into {@link Message}s.
   */
  Map<String, TypedValue<InputStream>> getAttachments();

}
