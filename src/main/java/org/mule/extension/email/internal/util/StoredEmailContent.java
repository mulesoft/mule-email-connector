/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static javax.mail.Part.ATTACHMENT;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.TEXT;
import static org.mule.runtime.api.metadata.DataType.HTML_STRING;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Given a {@link Message} that lives in a mailbox introspects it's content to obtain the body an the attachments if any.
 *
 * @since 1.0
 */
public class StoredEmailContent {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredEmailContent.class);

  public static final StoredEmailContent EMPTY = new StoredEmailContent();

  private final Map<String, TypedValue<InputStream>> attachmentParts = new LinkedHashMap<>();
  private final TypedValue<String> body;

  /**
   * Creates an instance and process the message content.
   *
   * @param message the {@link Message} to be processed.
   */
  public StoredEmailContent(Message message, StreamingHelper streamingHelper) {
    StringJoiner body = new StringJoiner("\n");
    processPart(message, body, streamingHelper);
    this.body = new TypedValue<>(body.toString().trim(), builder().type(String.class).mediaType(getMediaType(message)).build());
  }

  private StoredEmailContent() {
    this.body = new TypedValue<>("", HTML_STRING);
  }

  /**
   * @return the text body of the message.
   */
  public TypedValue<String> getBody() {
    return body;
  }

  /**
   * @return a {@link List} with the attachments of an email bounded into {@link Message}s.
   */
  public Map<String, TypedValue<InputStream>> getAttachments() {
    return ImmutableMap.copyOf(attachmentParts);
  }

  /**
   * Processes a single {@link Part} and adds it to the body of the message or as a new attachment depending on it's disposition
   * type.
   */
  private void processPart(Part part, StringJoiner bodyCollector, StreamingHelper streamingHelper) {
    try {
      Object content = part.getContent();
      if (isMultipart(content)) {
        Multipart mp = (Multipart) part.getContent();
        for (int i = 0; i < mp.getCount(); i++) {
          processPart(mp.getBodyPart(i), bodyCollector, streamingHelper);
        }
      }

      if (isAttachment(part)) {
        processAttachment(part, streamingHelper);
      } else {
        processBodyPart(part, bodyCollector, content);
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException("Error while processing message content.", e);
    }
  }

  /**
   * Processes a body part, adding it to the body that is being built.
   *
   * @param part the attachment part to be processed
   */
  private void processBodyPart(Part part, StringJoiner bodyCollector, Object content) throws MessagingException {
    if (isText(content)) {
      bodyCollector.add((String) content);
    }

    if (content instanceof InputStream && isInline(part) && part.isMimeType(TEXT)) {
      String inline = IOUtils.toString((InputStream) content);
      bodyCollector.add(inline);
    }
  }

  /**
   * Processes an attachment part, adding it to the attachments list.
   *
   * @param part the attachment part to be processed
   */
  private void processAttachment(Part part, StreamingHelper streamingHelper) throws MessagingException, IOException {
    Object content = streamingHelper.resolveCursorProvider(part.getInputStream());
    DataType dataType = builder().type(content.getClass()).mediaType(part.getContentType()).build();
    attachmentParts.put(part.getFileName(), new TypedValue(content, dataType));
  }

  /**
   * Evaluates whether the disposition of the {@link Part} is INLINE or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as inline, false otherwise
   */
  private boolean isInline(Part part) throws MessagingException {
    return !isAttachment(part);
  }

  /**
   * Evaluates whether a {@link Part} is an attachment or not.
   *
   * @param part the part to be validated.
   * @return true is the part is dispositioned as an attachment, false otherwise
   */
  private boolean isAttachment(Part part) throws MessagingException {
    return part.getFileName() != null && (part.getDisposition() == null || part.getDisposition().equals(ATTACHMENT));
  }

  /**
   * Evaluates whether a content is multipart or not.
   *
   * @param content the content to be evaluated.
   * @return true if is multipart, false otherwise
   */
  private boolean isMultipart(Object content) {
    return content instanceof Multipart;
  }

  /**
   * Evaluates whether a content is text or not.
   *
   * @param content the content to be evaluated.
   * @return true if is text, false otherwise
   */
  private boolean isText(Object content) {
    return content instanceof String;
  }

  private MediaType getMediaType(Message message) {
    try {
      String contentType = message.getContentType();
      return contentType == null ? ANY : MediaType.parse(contentType);
    } catch (MessagingException e) {
      LOGGER.error("Could not obtain the message content type", e);
      return ANY;
    }
  }
}
