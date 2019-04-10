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
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.slf4j.LoggerFactory;

/**
 * Models a body of MimeType 'multipart/related' or 'text/*'.
 *
 * @since 1.2.0
 */
public class SimpleBody implements MessageBody {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);
  private static final String MULTIPART_RELATED = MediaType.MULTIPART_RELATED.toRfcString();
  private static final String TEXT_ANY = MediaType.create("text", "*").toRfcString();

  /**
   * The text extracted from the given part.
   */
  private String text = "";

  /**
   * A collection of all the inline attachments present in the body.
   */
  private Collection<MessageAttachment> inlineAttachments = new ArrayList<>();

  /**
   * @param part the {@link Part} from which the message will be extracted.
   */
  public SimpleBody(Part part) {
    try {
      Object content;
      if (hasInlineAttachments(part)) {
        Multipart mp = getMultipart(part);
        content = extractBodyContent(mp);
        initInlineAttachments(mp);
      } else if (isTextBody(part)) {
        content = part.getContent();
      } else {
        LOGGER.debug(format("Expected MimeType of the part was either 'multipart/related' or 'text/*', but was: '%s'.",
                            part.getContentType()));
        return;
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
    return inlineAttachments;
  }

  private void initInlineAttachments(Multipart mp) throws MessagingException {
    for (int i = 1; i < mp.getCount(); i++) {
      inlineAttachments.add(new MessageAttachment(mp.getBodyPart(i)));
    }
  }

  private Object extractBodyContent(Multipart mp) throws IOException, MessagingException {
    return mp.getBodyPart(0).getContent();
  }

  private boolean hasInlineAttachments(Part part) throws MessagingException {
    return part.isMimeType(MULTIPART_RELATED);
  }

  private boolean isTextBody(Part part) throws MessagingException {
    return part.isMimeType(TEXT_ANY);
  }

}
