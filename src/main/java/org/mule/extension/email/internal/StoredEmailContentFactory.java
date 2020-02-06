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
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    EmailMessage email = new EmailMessage(message);
    String text = email.getText().trim();
    StringBuilder textBuilder = new StringBuilder(text);

    LinkedHashMap<String, TypedValue<InputStream>> processedAttachments = new LinkedHashMap<>();
    LinkedList<MessageAttachment> unnamedAttachments = new LinkedList<>();

    List<MessageAttachment> unprocessedAttachments = email.getAttachments();
    Collections.reverse(unprocessedAttachments); // This is done to avoid breaking backwards compatibility for attachments with the same name.
    for (MessageAttachment attachment : unprocessedAttachments) {
      Optional<String> attachmentName = attachment.getAttachmentName(attachmentNamingStrategy);
      if (attachmentName.isPresent()) {
        addNamedAttachment(processedAttachments, attachment, attachmentName.get(), textBuilder);
      } else {
        unnamedAttachments.add(attachment);
      }
    }
    processUnnamedAttachments(processedAttachments, unnamedAttachments, textBuilder);
    DataType dataType = builder().type(String.class).mediaType(getMediaType(message)).build();
    return new DefaultStoredEmailContent(new TypedValue<>(textBuilder.toString(), dataType), processedAttachments);
  }

  private void processUnnamedAttachments(LinkedHashMap<String, TypedValue<InputStream>> processedAttachments,
                                         LinkedList<MessageAttachment> unnamedAttachments, StringBuilder textBuilder) {
    Collections.reverse(unnamedAttachments); // This is done to avoid breaking backwards ordering of unnamed emails.
    for (MessageAttachment attachment : unnamedAttachments) {
      addNamedAttachment(processedAttachments, attachment, DEFAULT_NAME, textBuilder);
    }
  }

  private void addNamedAttachment(LinkedHashMap<String, TypedValue<InputStream>> processedAttachments,
                                  MessageAttachment attachment, String proposedName, StringBuilder textBuilder) {
    TypedValue<InputStream> content = resolveContent(attachment.getContent(), streamingHelper);
    String name = getUniqueAttachmentName(processedAttachments.keySet(), proposedName, content.getDataType());
    processedAttachments.put(name, content);
    Optional<String> contentId = extractContentID(attachment);
    contentId.ifPresent(s -> replaceAll(textBuilder, format(CID_MASK, s), format(CID_MASK, name)));
  }

  private void replaceAll(StringBuilder builder, String from, String to) {
    int index = builder.indexOf(from);
    while (index != -1) {
      builder.replace(index, index + from.length(), to);
      index += to.length(); // Move to the end of the replacement
      index = builder.indexOf(from, index);
    }
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
  private TypedValue<InputStream> resolveContent(Part part, StreamingHelper streamingHelper) {
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

  private String getUniqueAttachmentName(Set<String> keys, String attachmentName, DataType dataType) {
    if (keys.contains(attachmentName)) {
      String extension = "";
      String baseName = attachmentName;
      if (!dataType.getMediaType().toRfcString().contains("message/rfc822")) {
        int extensionDotIndex = attachmentName.lastIndexOf('.');
        if (extensionDotIndex != -1) {
          extension = attachmentName.substring(extensionDotIndex);
          baseName = attachmentName.substring(0, extensionDotIndex);
        }
      }
      return resolveUniqueName(keys, baseName, extension, 0);
    }
    return attachmentName;
  }

  private String resolveUniqueName(Set<String> keys, String baseName, String extension, Integer lastSuffixTried) {
    String candidateBaseName = baseName + "_" + ++lastSuffixTried + extension;
    if (keys.contains(candidateBaseName)) {
      return resolveUniqueName(keys, baseName, extension, lastSuffixTried);
    }
    return candidateBaseName;
  }
}
