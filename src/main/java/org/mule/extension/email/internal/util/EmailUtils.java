/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static java.nio.charset.Charset.forName;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.sender.EmailBody;
import org.mule.runtime.api.metadata.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;

import com.sun.mail.imap.IMAPInputStream;

/**
 * Utility class to share logic
 *
 * @since 1.0
 */
public class EmailUtils {

  private EmailUtils() {}

  public static final String TEXT_ANY = MediaType.create("text", "*").toRfcString();
  public static final String MULTIPART_MIXED = MediaType.MULTIPART_MIXED.toRfcString();
  public static final String MULTIPART_ALTERNATIVE = MediaType.create("multipart", "alternative").toRfcString();
  public static final String MULTIPART_RELATED = MediaType.MULTIPART_RELATED.toRfcString();

  /**
  * Resolves which is the {@link MediaType} that describes the body content.
  *
  * @param body email body which contains the information about the content's charset
  * @param configCharset the default charset to be used if the content charset and the operation override charset are not defined
  * @return the {@link MediaType} that describes the body content.
  */
  public static MediaType getMediaType(EmailBody body, String configCharset) {
    Charset charset = body.getContent()
        .getDataType()
        .getMediaType()
        .getCharset()
        .orElseGet(() -> forName(resolveOverride(configCharset, body.getOverrideEncoding())));

    MediaType mediaType = body.getContentType()
        .orElse(body.getContent().getDataType().getMediaType());

    if (mediaType.equals(ANY)) {
      mediaType = TEXT;
    }

    return mediaType.withCharset(charset);
  }

  public static <T> T resolveOverride(T configValue, T operationValue) {
    return operationValue == null ? configValue : operationValue;
  }

  /**
   * Processes a single {@link Part} which represent a MultiPart and returns its content as a {@link Multipart}.
   *
   * @param part the part to be processed.
   * @return the part's content as a {@link Multipart}.
   * @exception IllegalArgumentException if the input Part does not represent a MultiPart.
   * @exception EmailException for other failures.
   */
  public static Multipart getMultipart(Part part) {
    try {
      Object content = part.getContent();
      if (content instanceof IMAPInputStream || content instanceof SharedByteArrayInputStream) {
        return new MimeMultipart(part.getDataHandler().getDataSource());
      } else if (content instanceof InputStream) {
        ByteArrayDataSource fa = new ByteArrayDataSource(((InputStream) content), part.getContentType());
        return new MimeMultipart(fa);
      } else if (content instanceof Multipart) {
        return (Multipart) content;
      } else {
        throw new IllegalArgumentException("The expected content of the part is not a multipart.");
      }
    } catch (MessagingException | IOException e) {
      throw new EmailException("Could not obtain the part's content", e);
    }
  }

  public static boolean hasBodyAndAttachments(Part message) throws MessagingException {
    return message.isMimeType(MULTIPART_MIXED);
  }

  public static boolean hasAlternativeBodies(Part part) throws MessagingException {
    return part.isMimeType(MULTIPART_ALTERNATIVE);
  }

  public static boolean hasInlineAttachments(Part part) throws MessagingException {
    return part.isMimeType(MULTIPART_RELATED);
  }

  public static boolean isTextBody(Part part) throws MessagingException {
    return part.isMimeType(TEXT_ANY);
  }

}
