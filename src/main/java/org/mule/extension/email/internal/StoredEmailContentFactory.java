/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.metadata.DataType.builder;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
import org.mule.extension.email.internal.util.DefaultMailPartContentResolver;
import org.mule.extension.email.internal.util.MailPartContentResolver;
import org.mule.extension.email.internal.util.message.EmailMessage;
import org.mule.extension.email.internal.util.message.MessageAttachment;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Optional;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link StoredEmailContent} instances.
 *
 * @since 1.1
 */
public class StoredEmailContentFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);
  private static final String CID_MASK = "\"cid:%s\"";

  private MailPartContentResolver contentResolver = new DefaultMailPartContentResolver();

  public static final StoredEmailContent EMPTY = new DefaultStoredEmailContent(new TypedValue("", DataType.STRING), emptyMap());
  public static final String DEFAULT_NAME = "Unnamed";
  private StreamingHelper streamingHelper;

  public StoredEmailContentFactory(StreamingHelper streamingHelper) {
    this.streamingHelper = streamingHelper;
  }

  public StoredEmailContentFactory() {}

  /**
   * Creates an instance and processes the message content.
   *
   * @param message the {@link Message} to be processed.
   */
  public StoredEmailContent fromMessage(Message message, AttachmentNamingStrategy attachmentNamingStrategy) {
    String defaultName = DEFAULT_NAME;
    int i = 0;
    EmailMessage email = new EmailMessage(message);
    String text = email.getText().trim();

    LinkedHashMap<String, TypedValue<InputStream>> namedAttachments = new LinkedHashMap<>();
    for (MessageAttachment attachment : email.getAttachments()) {
      if (namedAttachments.containsKey(defaultName)) {
        defaultName = DEFAULT_NAME + "_" + ++i;
      }
      TypedValue<InputStream> content = resolveAttachment(attachment.getContent(), streamingHelper);
      String attachmentName = attachment.getAttachmentName(defaultName, attachmentNamingStrategy);
      namedAttachments.put(attachmentName, content);

      Optional<String> contentId = extractContentID(attachment);
      if (contentId.isPresent()) {
        text = text.replace(format(CID_MASK, contentId.get()), format(CID_MASK, attachmentName));
      }
    }

    DataType dataType = builder().type(String.class).mediaType(getMediaType(message)).build();
    return new DefaultStoredEmailContent(new TypedValue<>(text, dataType), namedAttachments);
  }

  private Optional<String> extractContentID(MessageAttachment attachment) {
    try {
      Enumeration<Header> headers = attachment.getContent().getAllHeaders();
      while (headers.hasMoreElements()) {
        Header header = headers.nextElement();
        if (header.getName().equalsIgnoreCase("content-id") && header.getValue() != null) {
          return of(header.getValue().replaceAll("^<?([^>]+)>?$", "$1"));
        }
      }
    } catch (MessagingException e) {
      // ignore
    }
    return empty();
  }

  /**
   * @param part the content to be resolved.
   * @param streamingHelper helps resolve the content for attachments.
   * @return the attachment's content as a {@link TypedValue}.
   */
  private TypedValue<InputStream> resolveAttachment(Part part, StreamingHelper streamingHelper) {
    try {
      InputStream partContent = contentResolver.resolveInputStream(part);
      Object content = streamingHelper != null ? streamingHelper.resolveCursorProvider(partContent) : partContent;
      DataType dataType = builder().type(content.getClass()).mediaType(part.getContentType()).build();
      return new TypedValue(content, dataType);
    } catch (MessagingException | IOException e) {
      throw new EmailException("Could not resolve the attachment", e);
    }
  }

  private static MediaType getMediaType(Message message) {
    try {
      MediaType mediaType = MediaType.parse(message.getContentType());
      if (!"multipart".equals(mediaType.getPrimaryType())) {
        return mediaType;
      } else {
        return MediaType.TEXT;
      }
    } catch (MessagingException e) {
      LOGGER.error("Could not obtain the message content type", e);
      return MediaType.TEXT;
    }
  }
}
