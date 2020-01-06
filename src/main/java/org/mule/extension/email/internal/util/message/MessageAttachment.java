/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import org.mule.extension.email.api.exception.EmailException;

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;

/**
 * A wrapper for {@link Part}s that represent an attachment.
 *
 * @since 1.2.0
 */
public class MessageAttachment {

  private static final Pattern NAME_HEADER = Pattern.compile("^name=\"(.+)\"");
  private Part content;

  public MessageAttachment(Part part) {
    content = part;
  }

  /**
   * @return the attachment's content as a {@link Part}.
   */
  public Part getContent() {
    return content;
  }

  /**
   * @param defaultName the default name if the attachment does not have one.
   * @return the attachment's name. If it has no name, it will return the given {@code defaultName}.
   */
  public String getAttachmentName(String defaultName) {
    try {
      String fileName = content.getFileName();
      if (fileName == null || fileName.trim().isEmpty()) {
        Enumeration<Header> headers = content.getAllHeaders();
        while (headers.hasMoreElements()) {
          Header header = headers.nextElement();
          Matcher matcher = NAME_HEADER.matcher(header.getName());
          if (matcher.matches()) {
            fileName = matcher.group(1);
            break;
          }
        }
      }

      return fileName != null && !fileName.trim().isEmpty() ? fileName : defaultName;

    } catch (MessagingException e) {
      throw new EmailException("Error file trying to get the attachment's name", e);
    }
  }

}
