/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.core.api.util.IOUtils;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailUtils.*;

/**
 * Models a body of MimeType 'text/*'.
 *
 * @since 1.2.0
 */
public class TextBody implements MessageBody {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);

  /**
   * The text extracted from the given part.
   */
  private String text = "";

  /**
   * @param part the {@link Part} from which the message will be extracted.
   */
  public TextBody(Part part) {
    try {
      if (isTextBody(part)) {
        Object content = part.getContent();
        text = (content instanceof InputStream ? IOUtils.toString((InputStream) content) : (String) content);
      } else if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Expected MimeType of the part was 'text/*', but was: '%s'.",
                            part.getContentType()));
      }
    } catch (IOException | MessagingException e) {
      throw new EmailException("Could not process simple message part", e);
    }
  }

  public String getText() {
    return text;
  }

  public Collection<MessageAttachment> getInlineAttachments() {
    return Collections.emptySet();
  }

}
