/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.metadata.DataType.builder;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.exception.EmailException;
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
import java.util.Collection;
import java.util.LinkedHashMap;

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
  public StoredEmailContent fromMessage(Message message) {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    if (currentClassLoader != getClass().getClassLoader()) {
      LOGGER.warn("Incorrect class loader. Switching to the right one.");
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }
    EmailMessage email = new EmailMessage(message);
    TypedValue<String> body =
        new TypedValue<>(email.getText().trim(), builder().type(String.class).mediaType(getMediaType(message)).build());
    LinkedHashMap<String, TypedValue<InputStream>> attachments = getNamedAttachments(email.getAttachments());
    return new DefaultStoredEmailContent(body, attachments);
  }

  /**
   * Processes a collection of attachments by obtaining their names and resolving their content, to return a map with
   * that information.
   *
   * @param attachments the collection of {@link MessageAttachment}s
   * @return a map with the attachment's name as the key and the resolved attachment's content as the value.
   */
  private LinkedHashMap<String, TypedValue<InputStream>> getNamedAttachments(Collection<MessageAttachment> attachments) {
    String defaultName = DEFAULT_NAME;
    Integer i = 1;
    LinkedHashMap<String, TypedValue<InputStream>> namedAttachments = new LinkedHashMap<>();
    for (MessageAttachment attachment : attachments) {
      if (namedAttachments.containsKey(defaultName)) {
        defaultName = DEFAULT_NAME + "_" + i++;
      }
      TypedValue<InputStream> content = resolveAttachment(attachment.getContent(), streamingHelper);
      namedAttachments.put(attachment.getAttachmentName(defaultName), content);
    }
    return namedAttachments;
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
