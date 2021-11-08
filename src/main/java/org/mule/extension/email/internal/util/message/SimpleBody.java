/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailUtils.getMultipart;
import static org.mule.extension.email.internal.util.EmailUtils.hasAlternativeBodies;
import static org.mule.extension.email.internal.util.EmailUtils.hasInlineAttachments;
import static org.mule.extension.email.internal.util.EmailUtils.isTextBody;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.StoredEmailContentFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.LoggerFactory;

/**
 * Models a body of MimeType 'multipart/related' or 'text/*'.
 *
 * @since 1.2.0
 */
public class SimpleBody implements MessageBody {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);
  private static final String NAME_HEADER = "name=";
  private static final String ATTACHMENT = "attachment";


  /**
   * The text extracted from the given part.
   */
  private MessageBody body;

  /**
   * A collection of all the inline attachments present in the body.
   */
  private Collection<MessageAttachment> inlineAttachments = new ArrayList<>();

  /**
   * @param part the {@link Part} from which the message will be extracted.
   */
  public SimpleBody(Part part) {
    try {
      Part bodyPart;
      if (hasInlineAttachments(part)) {
        Multipart mp = getMultipart(part);
        bodyPart = mp.getBodyPart(0);
        initInlineAttachments(mp);
      } else if (isTextBody(part)) {
        if (part.getDisposition() != null && part.getDisposition().contains(ATTACHMENT)) {
          inlineAttachments.add(new MessageAttachment(part));
          bodyPart = new MimeBodyPart();
          bodyPart.setText("");
        } else {
          bodyPart = part;
        }
      } else {
        if ((part.getDisposition() != null && part.getDisposition().contains(ATTACHMENT)) ||
            part.getContentType().contains(NAME_HEADER)) {
          inlineAttachments.add(new MessageAttachment(part));
          bodyPart = new MimeBodyPart();
          bodyPart.setText("");
        } else {
          bodyPart = new MimeBodyPart(new ByteArrayInputStream(new byte[0]));
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Expected MimeType of the part was either 'multipart/related' or 'text/*', but was: '%s'.",
                                part.getContentType()));
          }
        }
      }
      body = hasAlternativeBodies(bodyPart) ? new AlternativeBody(bodyPart) : new TextBody(bodyPart);
      inlineAttachments.addAll(body.getInlineAttachments());
    } catch (MessagingException e) {
      throw new EmailException("Could not process simple message part", e);
    }
  }

  public String getText() {
    return body.getText();
  }

  public Collection<MessageAttachment> getInlineAttachments() {
    return inlineAttachments;
  }

  private void initInlineAttachments(Multipart mp) throws MessagingException {
    for (int i = 1; i < mp.getCount(); i++) {
      inlineAttachments.add(new MessageAttachment(mp.getBodyPart(i)));
    }
  }
}
