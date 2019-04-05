/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.util.Collections.emptyMap;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.MULTIPART_RELATED;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.util.DefaultMailPartContentResolver;
import org.mule.extension.email.internal.util.MailPartContentResolver;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import com.sun.mail.imap.IMAPInputStream;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;

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
   * type. Only the first part of the message or the parts in a multipart/alternative message are considered as body parts, everything
   * else is considered as an attachment, whether is has Content-Disposition or not.
   *
   * @param part the part to be processed.
   * @param bodyCollector collects the text from the body parts as they are processed and builds the final text.
   * @param attachments collects the attachments as each part is processed.
   * @param streamingHelper helps resolve the content for attachments.
   */
  private void processPart(Part part, StringJoiner bodyCollector, Map<String, TypedValue<InputStream>> attachments,
                           StreamingHelper streamingHelper) {
    try {
      if (part.isMimeType("multipart/*")) {
        Multipart mp = getMultipart(part);
        if (part.isMimeType("multipart/alternative")) {
          for (int i = 0; i < mp.getCount(); i++) {
            processBodyPart(mp.getBodyPart(i), bodyCollector, attachments, streamingHelper);
          }
        } else {
          processBodyPart(mp.getBodyPart(0), bodyCollector, attachments, streamingHelper);
          for (int i = 1; i < mp.getCount(); i++) {
            processAttachment(mp.getBodyPart(i), attachments, streamingHelper);
          }
        }
      } else if (part.isMimeType("text/*")) {
        processBodyPart(part, bodyCollector, attachments, streamingHelper);
      } else {
        LOGGER.error("Error processing part. Invalid/Unrecognized MimeType: " + part.getContentType());
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException("Error while processing the message contents.", e);
    }
  }

  /**
   * Processes a body part, adding its text to the body that is being built.
   *
   * @param part the part to be processed.
   * @param bodyCollector collects the text from the body parts as they are processed and builds the final text.
   * @param attachments collects the attachments as each part is processed.
   * @param streamingHelper helps resolve the content for attachments.
   */
  private void processBodyPart(Part part, StringJoiner bodyCollector, Map<String, TypedValue<InputStream>> attachments,
                               StreamingHelper streamingHelper) {
    try {
      if (part.isMimeType("multipart/alternative")) {
        Multipart mp = getMultipart(part);
        for (int i = 0; i < mp.getCount(); i++) {
          processBodyPart(mp.getBodyPart(i), bodyCollector, attachments, streamingHelper);
        }
      } else if (part.isMimeType("multipart/related")) {
        Multipart mp = getMultipart(part);
        processBodyPart(mp.getBodyPart(0), bodyCollector, attachments, streamingHelper);
        for (int i = 1; i < mp.getCount(); i++) {
          processAttachment(mp.getBodyPart(i), attachments, streamingHelper);
        }
      } else if (part.isMimeType("text/*")) {
        Object content = part.getContent();
        if (content instanceof String) {
          bodyCollector.add((String) content);
        } else if (content instanceof InputStream) {
          String inline = IOUtils.toString((InputStream) content);
          bodyCollector.add(inline);
        }
      } else {
        LOGGER.error("Error processing body part. Invalid/Unrecognized MimeType: " + part.getContentType());
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException("Error while processing the message body.", e);
    }
  }

  /**
   * Processes a single {@link Part} and returns its content as a {@link MimeMultipart}.
   *
   * @param part the part to be processed.
   * @return the part's content as a {@link MimeMultipart}.
   */
  private MimeMultipart getMultipart(Part part) {
    try {
      Object content = part.getContent();
      if (content instanceof InputStream) {
        ByteArrayDataSource fa = new ByteArrayDataSource(((InputStream) content), part.getContentType());
        return new MimeMultipart(fa);
      } else if (content instanceof IMAPInputStream || content instanceof SharedByteArrayInputStream) {
        return new MimeMultipart(part.getDataHandler().getDataSource());
      } else {
        return (MimeMultipart) content;
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException("Could not convert the part's content to a MimeMultipart", e);
    }
  }

  /**
   * Processes an attachment part, adding it to the attachments list.
   *
   * @param part the attachment part to be processed.
   * @param attachments collects the attachment.
   * @param streamingHelper helps resolve the content for attachments.
   */
  private void processAttachment(Part part, Map<String, TypedValue<InputStream>> attachments, StreamingHelper streamingHelper)
      throws MessagingException, IOException {
    InputStream partContent = contentResolver.resolveInputStream(part);
    Object content =
        streamingHelper != null ? streamingHelper.resolveCursorProvider(partContent) : partContent;
    DataType dataType = builder().type(content.getClass()).mediaType(part.getContentType()).build();
    attachments.put(part.getFileName(), new TypedValue(content, dataType));
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
