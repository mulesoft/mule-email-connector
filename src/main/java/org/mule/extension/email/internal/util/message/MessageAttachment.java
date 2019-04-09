/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import org.mule.extension.email.api.exception.EmailException;

import javax.mail.MessagingException;
import javax.mail.Part;

public class MessageAttachment {

  private Part content;

  public MessageAttachment(Part part) {
    content = part;
  }

  public Part getContent() {
    return content;
  }

  public String getAttachmentName(String defaultName) {
    try {
      if (content.getFileName() != null) {
        return content.getFileName();
      } else {
        return defaultName;
      }
    } catch (MessagingException e) {
      throw new EmailException("Error file trying to get the attachment's name", e);
    }
  }

}
