/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailUtils.getMultipart;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * Models a body of MimeType 'multipart/related' or 'text/*'.
 *
 * @since 1.2.0
 */
public class SimpleBody implements MessageBody {

  /**
   * The text extracted from the given part.
   */
  private String text;

  /**
   * A collection of all the inline attachments present in the body.
   */
  private Collection<MessageAttachment> attachments = new ArrayList<>();

  /**
   * @param part the {@link Part} from which the message will be extracted.
   */
  public SimpleBody(Part part) {
    try {
      Object content;
      if (part.isMimeType("multipart/related")) {
        Multipart mp = getMultipart(part);
        content = mp.getBodyPart(0).getContent();
        for (int i = 1; i < mp.getCount(); i++) {
          attachments.add(new MessageAttachment(mp.getBodyPart(i)));
        }
      } else if (part.isMimeType("text/*")) {
        content = part.getContent();
      } else {
        throw new IllegalArgumentException(format("Expected MimeType of the part was either 'multipart/related' or 'text/*', but was: '%s'.",
                                                  part.getContentType()));
      }
      text = (content instanceof InputStream ? IOUtils.toString((InputStream) content) : (String) content);
    } catch (IOException | MessagingException e) {
      throw new EmailException("Could not process simple message part", e);
    }
  }

  public String getText() {
    return text;
  }

  public Collection<MessageAttachment> getInlineAttachments() {
    return attachments;
  }

}
