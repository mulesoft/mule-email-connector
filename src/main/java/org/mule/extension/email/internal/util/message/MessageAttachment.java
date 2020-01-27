/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import static java.util.Optional.*;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME_HEADERS;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
import org.mule.extension.email.api.exception.EmailException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;

/**
 * A wrapper for {@link Part}s that represent an attachment.
 *
 * @since 1.2.0
 */
public class MessageAttachment {

  private static final Logger LOGGER = getLogger(MessageAttachment.class);
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
   * @param attachmentNamingStrategy  The strategy that must be used when searching for the attachment name.
   * @return the attachment's name. If it has no name, it will return the given {@code defaultName}.
   */
  public Optional<String> getAttachmentName(AttachmentNamingStrategy attachmentNamingStrategy) {
    return getNamingStrategy(attachmentNamingStrategy)
        .getAttachmentName();
  }

  private NamingStrategy getNamingStrategy(AttachmentNamingStrategy attachmentNamingStrategy) {
    if (attachmentNamingStrategy.equals(NAME)) {
      return new FilenameStrategy();
    } else if (attachmentNamingStrategy.equals(NAME_HEADERS)) {
      return new FilenameHeadersStrategy();
    } else {
      return new FilenameHeadersSubjectStrategy();
    }
  }

  private abstract static class NamingStrategy {

    public Optional<String> getAttachmentName() {
      try {
        String attachmentName = doGetAttachmentName();
        return isNotBlank(attachmentName) ? of(attachmentName) : empty();
      } catch (MessagingException e) {
        throw new EmailException("Error file trying to get the attachment's name", e);
      }
    }

    protected abstract String doGetAttachmentName() throws MessagingException;
  }

  private class FilenameStrategy extends NamingStrategy {

    @Override
    public String doGetAttachmentName() throws MessagingException {
      return content.getFileName();
    }
  }

  private class FilenameHeadersStrategy extends FilenameStrategy {

    @Override
    public String doGetAttachmentName() throws MessagingException {
      String name = super.doGetAttachmentName();

      return isNotBlank(name) ? name : getNameFromHeaders();
    }

    private String getNameFromHeaders() throws MessagingException {
      Enumeration<Header> headers = content.getAllHeaders();
      while (headers.hasMoreElements()) {
        Header header = headers.nextElement();
        Matcher matcher = NAME_HEADER.matcher(header.getName());
        if (matcher.matches()) {
          return matcher.group(1);
        }
      }

      return EMPTY;
    }
  }

  private class FilenameHeadersSubjectStrategy extends FilenameHeadersStrategy {

    @Override
    public String doGetAttachmentName() throws MessagingException {
      String name = super.doGetAttachmentName();

      return isNotBlank(name) ? name : getNameFromSubject();
    }

    private String getNameFromSubject() throws MessagingException {
      if (content instanceof BodyPart) {
        Object nestedMessage;
        try {
          nestedMessage = content.getDataHandler().getContent();
          if (nestedMessage instanceof MimeMessage) {
            String subject = ((MimeMessage) nestedMessage).getSubject();
            if (isNotBlank(subject)) {
              return subject;
            }
          }
        } catch (IOException e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Could not get attachment name from data handler", e);
          }
        }
      }

      return EMPTY;
    }
  }
}
