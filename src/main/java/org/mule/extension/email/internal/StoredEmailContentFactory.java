/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static com.google.common.net.MediaType.OCTET_STREAM;
import static java.util.Collections.emptyMap;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.MULTIPART_RELATED;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.util.EmailConnectorConstants;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * Factory for {@link StoredEmailContent} instances.
 *
 * @since 1.1
 */
public class StoredEmailContentFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredEmailContentFactory.class);

  public static final StoredEmailContent EMPTY = new DefaultStoredEmailContent(new TypedValue("", DataType.STRING), emptyMap());
  private StreamingHelper streamingHelper;

  public StoredEmailContentFactory(StreamingHelper streamingHelper) {
    this.streamingHelper = streamingHelper;
  }

  public StoredEmailContentFactory() {}

  /**
   * Creates an instance and process the message content.
   *
   * @param message the {@link Message} to be processed.
   */
  public StoredEmailContent fromMessage(Message message) {
    StringJoiner bodyBuilder = new StringJoiner("\n");
    LinkedHashMap<String, TypedValue<InputStream>> attachments = new LinkedHashMap<>();
    processPart(message, bodyBuilder, attachments, streamingHelper);
    TypedValue<String> body =
        new TypedValue<>(bodyBuilder.toString().trim(), builder().type(String.class).mediaType(getMediaType(message)).build());
    return new DefaultStoredEmailContent(body, attachments);
  }

  /**
   * Processes a single {@link Part} and adds it to the body of the message or as a new attachment depending on it's disposition
   * type.
   */
  private void processPart(Part part, StringJoiner bodyCollector, Map<String, TypedValue<InputStream>> attachments,
                           StreamingHelper streamingHelper) {
    try {
      Object content = part.getContent();
      if (isMultipart(part)) {
        Multipart mp;
        if (content instanceof InputStream) {
          ByteArrayDataSource fa = new ByteArrayDataSource(((InputStream) content), part.getContentType());
          mp = new MimeMultipart(fa);
        } else {
          mp = (Multipart) content;
        }
        for (int i = 0; i < mp.getCount(); i++) {
          processPart(mp.getBodyPart(i), bodyCollector, attachments, streamingHelper);
        }
      } else {
        if (isAttachment(part)) {
          processAttachment(part, attachments, streamingHelper);
        } else {
          processBodyPart(part, bodyCollector, attachments, content, streamingHelper);
        }
      }

    } catch (MessagingException | IOException e) {
      throw new EmailException("Error while processing message content.", e);
    }
  }

  /**
   * Processes a body part, adding it to the body that is being built.
   *
   * @param part the attachment part to be processed
   * @param attachments
   */
  private void processBodyPart(Part part, StringJoiner bodyCollector, Map<String, TypedValue<InputStream>> attachments,
                               Object content, StreamingHelper streamingHelper)
      throws MessagingException {
    if (isText(content)) {
      bodyCollector.add((String) content);
    } else if (content instanceof InputStream && isInline(part) && part.isMimeType(EmailConnectorConstants.TEXT)) {
      String inline = IOUtils.toString((InputStream) content);
      bodyCollector.add(inline);
    } else if (content instanceof IMAPInputStream) {
      MimeMultipart mp = new MimeMultipart(part.getDataHandler().getDataSource());
      for (int i = 0; i < mp.getCount(); i++) {
        processPart(mp.getBodyPart(i), bodyCollector, attachments, streamingHelper);
      }
    } else if (content instanceof MimeMultipart) {
      MimeMultipart mp = (MimeMultipart) content;
      for (int i = 0; i < mp.getCount(); i++) {
        processPart(mp.getBodyPart(i), bodyCollector, attachments, streamingHelper);
      }
    }
  }

  /**
   * Processes an attachment part, adding it to the attachments list.
   *
   * @param part the attachment part to be processed
   * @param attachments
   */
  private void processAttachment(Part part, Map<String, TypedValue<InputStream>> attachments, StreamingHelper streamingHelper)
      throws MessagingException, IOException {
    Object content =
        streamingHelper != null ? streamingHelper.resolveCursorProvider(part.getInputStream()) : part.getInputStream();
    DataType dataType = builder().type(content.getClass()).mediaType(part.getContentType()).build();
    attachments.put(part.getFileName(), new TypedValue(content, dataType));
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
    return part.getFileName() != null
        && (part.getDisposition() == null || (part.getDisposition().equalsIgnoreCase(ATTACHMENT)) || isInlineAttachment(part));
  }

  private boolean isInlineAttachment(Part part) throws MessagingException {
    return part.getDisposition().equalsIgnoreCase(INLINE) && part.isMimeType(OCTET_STREAM.toString());
  }

  /**
   * Evaluates whether a content is multipart or not.
   *
   * @param part the content to be evaluated.
   * @return true if is multipart, false otherwise
   */
  private boolean isMultipart(Part part) throws IOException, MessagingException {
    return part.getContent() instanceof Multipart || part.getContentType().contains(MULTIPART_RELATED.getPrimaryType());
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
