/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import static org.mule.extension.email.internal.util.EmailUtils.getMultipart;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.StoredEmailContentFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.slf4j.LoggerFactory;

/**
 * Abstraction to represent a email message, exposing its body text and attachments.
 *
 * @since 1.2.0
 */
public class EmailMessage {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);

  private MessageBody body;

  private Collection<MessageAttachment> attachments = new ArrayList<>();

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

  public Collection<MessageAttachment> getAttachments() {
    return attachments;
  }

  private void initMultipartEmail(Part message) throws MessagingException {
    Multipart mp = getMultipart(message);
    initBody(mp.getBodyPart(0));
    for (int i = 1; i < mp.getCount(); i++) {
      attachments.add(new MessageAttachment(mp.getBodyPart(i)));
    }
  }

  private void initBody(Part part) throws MessagingException {
    body = hasAlternativeBodies(part) ? new AlternativeBody(part) : new SimpleBody(part);
  }

  private boolean hasBodyAndAttachments(Part message) throws MessagingException {
    return message.isMimeType("multipart/mixed");
  }

  private boolean hasAlternativeBodies(Part part) throws MessagingException {
    return part.isMimeType("multipart/alternative");
  }

}
