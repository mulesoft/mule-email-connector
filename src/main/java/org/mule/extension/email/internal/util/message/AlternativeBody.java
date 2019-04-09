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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

public class AlternativeBody implements MessageBody {

  /**
   * Specifies the {@link Part} from which the message will be extracted.
   */
  private Part part;

  /**
   * A list of all the alternative bodies in the multipart.
   */
  private List<SimpleBody> bodies = new ArrayList<>();

  public AlternativeBody(Part part) {
    this.part = part;
    try {
      if (part.isMimeType("multipart/alternative")) {
        Multipart mp = getMultipart(part);
        for (int i = 0; i < mp.getCount(); i++) {
          bodies.add(new SimpleBody(mp.getBodyPart(i)));
        }
      } else {
        throw new IllegalArgumentException(format("Expected MimeType of the part was 'multipart/alternative', but was: '%s'.",
                                                  part.getContentType()));
      }
    } catch (MessagingException e) {
      throw new EmailException("Could not process alternative message part", e);
    }
  }

  public String getText() {
    String text = "";
    for (SimpleBody body : bodies) {
      text = text + "\n" + body.getText();
    }
    return text;
  }

  public Collection<MessageAttachment> getInlineAttachments() {
    Collection<MessageAttachment> attachments = new ArrayList<>();
    for (SimpleBody body : bodies) {
      attachments.addAll(body.getInlineAttachments());
    }
    return attachments;
  }

}
