/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import static org.mule.extension.email.internal.util.EmailUtils.getMultipart;
import static org.mule.extension.email.internal.util.EmailUtils.hasAlternativeBodies;
import static org.mule.extension.email.internal.util.EmailUtils.hasBodyAndAttachments;

import org.mule.extension.email.api.exception.EmailException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

/**
 * Abstraction to represent a email message, exposing its body text and attachments.
 *
 * @since 1.2.0
 */
public class EmailMessage {

  private MessageBody body;

  private List<MessageAttachment> attachments = new ArrayList<>();

  public EmailMessage(Part message) {
    try {
      if (hasBodyAndAttachments(message)) {
        initMultipartEmail(message);
      } else {
        initBody(message);
      }
    } catch (MessagingException e) {
      throw new EmailException("Error while processing the message contents.", e);
    }
    attachments.addAll(body.getInlineAttachments());
  }

  public String getText() {
    return body.getText();
  }

  public List<MessageAttachment> getAttachments() {
    return attachments;
  }

  private void initMultipartEmail(Part message) throws MessagingException {
    Multipart mp = getMultipart(message);
    boolean initialized = false;
    for (int i = 0; i < mp.getCount(); i++) {
      BodyPart p = mp.getBodyPart(i);
      if (!initialized && (p.getDisposition() == null || !p.getDisposition().equalsIgnoreCase(BodyPart.ATTACHMENT))) {
        initBody(p);
        initialized = true;
      } else {
        attachments.add(new MessageAttachment(p));
      }
    }
    if (body == null) {
      initBody(new MimeBodyPart(new ByteArrayInputStream(new byte[0])));
    }
  }

  private void initBody(Part part) throws MessagingException {
    body = hasAlternativeBodies(part) ? new AlternativeBody(part) : new SimpleBody(part);
  }

}
