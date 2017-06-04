/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.lang.String.format;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TRANSFER_ENCODING_HEADER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TYPE_HEADER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.MULTIPART;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import org.mule.extension.email.api.exception.EmailAttachmentException;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * an implementation of the builder design pattern to create a new {@link Message} instance.
 *
 * @since 1.0
 */
public final class MessageBuilder {

  private static final String ERROR = "Error while creating Message";

  private final MimeMessage message;

  private Map<String, TypedValue> attachments = new HashMap<>();
  private String content = "";
  private MediaType bodyContentType;
  private String attachmentContentTransferEncoding;
  private String bodyContentTransferEncoding;

  private MessageBuilder(Session s) throws MessagingException {
    this.message = new MimeMessage(s);
  }

  /**
   * Creates a new {@link MessageBuilder} instance for the specified {@code session}.
   *
   * @param session the {@link Session} for which the message is going to be created
   * @return a new {@link MessageBuilder} instance.
   */
  public static MessageBuilder newMessage(Session session) {
    try {
      return new MessageBuilder(session);
    } catch (MessagingException e) {
      throw new EmailException(ERROR, e);
    }
  }

  /**
   * Adds the subject to the {@link Message} that is being built.
   *
   * @param subject the subject of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withSubject(String subject) throws MessagingException {
    this.message.setSubject(subject);
    return this;
  }

  /**
   * Adds the from addresses to the {@link Message} that is being built.
   *
   * @param fromAddresses the from addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder fromAddresses(List<String> fromAddresses) throws MessagingException {
    this.message.addFrom(toAddressArray(fromAddresses));
    return this;
  }

  /**
   * Adds a single from address to the {@link Message} that is being built.
   *
   * @param from the from address of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder fromAddresses(String from) throws MessagingException {
    if (from != null) {
      this.message.setFrom(toAddress(from));
    } else {
      this.message.setFrom();
    }
    return this;
  }

  /**
   * Adds the "to" (primary) addresses to the {@link Message} that is being built.
   *
   * @param toAddresses the primary addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder to(List<String> toAddresses) throws MessagingException {
    if (toAddresses != null) {
      this.message.setRecipients(TO, toAddressArray(toAddresses));
    }
    return this;
  }

  /**
   * Adds the "bcc" addresses to the {@link Message} that is being built.
   *
   * @param bccAddresses the blind carbon copy addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder bcc(List<String> bccAddresses) throws MessagingException {
    if (bccAddresses != null) {
      this.message.setRecipients(BCC, toAddressArray(bccAddresses));
    }
    return this;
  }

  /**
   * Adds the "cc" addresses to the {@link Message} that is being built.
   *
   * @param ccAddresses the carbon copy addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder cc(List<String> ccAddresses) throws MessagingException {

    if (ccAddresses != null) {
      this.message.setRecipients(CC, toAddressArray(ccAddresses));
    }
    return this;
  }

  /**
   * Adds custom headers to the {@link Message} that is being built.
   *
   * @param headers the custom headers of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withHeaders(Map<String, String> headers) throws MessagingException {
    if (headers != null) {
      for (Entry<String, String> entry : headers.entrySet()) {
        this.message.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Adds attachments to the {@link Message} that is being built.
   *
   * @param attachments the attachments that are going to be added to the email.
   * @param contentTransferEncoding is used to indicate the type of transformation that has been used in order to
   *                                represent the attachments in an acceptable manner for transport.
   * @return this {@link MessageBuilder}
   */
  public MessageBuilder withAttachments(Map<String, TypedValue<InputStream>> attachments, String contentTransferEncoding) {
    this.attachmentContentTransferEncoding = contentTransferEncoding;
    this.attachments.putAll(attachments);
    return this;
  }

  /**
   * Adds the sent date to the {@link Message} that is being built.
   *
   * @param date the date in which the email was sent.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withSentDate(Date date) throws MessagingException {
    this.message.setSentDate(date);
    return this;
  }

  /**
   * Adds the text content the {@link Message} that is being built.
   *
   * @param content the text content of the email. If {@code null} is received, {@link StringUtils#EMPTY} will be used instead.
   * @param contentType the bodyContentType of the {@code content} of the email. One of "text/plain" or "text/html" expected.
   * @param contentTransferEncoding is used to indicate the type of transformation that has been used in order to
   *                                represent the body in an acceptable manner for transport.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withBody(String content, MediaType contentType, String contentTransferEncoding)
      throws MessagingException {
    this.content = content;
    this.bodyContentType = contentType;
    this.bodyContentTransferEncoding = contentTransferEncoding;
    return this;
  }

  /**
   * Adds the text content the {@link Message} that is being built. The bodyContentType of this content will default to "text/plain".
   *
   * @param content the text content of the email. If {@code null} is received, {@link StringUtils#EMPTY} will be used instead.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder withBody(String content) throws MessagingException {
    this.content = content;
    this.bodyContentType = HTML;
    return this;
  }

  /**
   * Adds the reply to addresses of the {@link Message} that is being built.
   *
   * @param replyAddresses the reply to addresses of the email.
   * @return this {@link MessageBuilder}
   * @throws MessagingException
   */
  public MessageBuilder replyTo(List<String> replyAddresses) throws MessagingException {
    this.message.setReplyTo(toAddressArray(replyAddresses));
    return this;
  }

  /**
   * Builds the {@link Message} with all the data provided.
   *
   * @return a new {@link MimeMessage} instance.
   * @throws MessagingException when a problem occurs creating a new {@link MimeMessage}
   */
  public MimeMessage build() throws MessagingException {
    String bodyContentType = getBodyPartContentType();
    if (attachments != null && !attachments.isEmpty()) {
      MimeMultipart multipart = new MimeMultipart();
      MimeBodyPart body = new MimeBodyPart();
      body.setDisposition(INLINE);
      body.setContent(content, bodyContentType);
      body.setHeader(CONTENT_TYPE_HEADER, bodyContentType);
      multipart.addBodyPart(body);
      attachments.entrySet().forEach(attachment -> addAttachment(multipart, attachment));
      message.setContent(multipart, MULTIPART);
    } else {
      message.setDisposition(INLINE);
      message.setContent(content, bodyContentType);
      message.setHeader(CONTENT_TRANSFER_ENCODING_HEADER, bodyContentTransferEncoding);
    }
    return message;
  }

  /**
   * @return the final email body content type.
   */
  private String getBodyPartContentType() {
    return bodyContentType.toRfcString();
  }

  private void addAttachment(MimeMultipart multipart, Entry<String, TypedValue> attachment) {
    MimeBodyPart part = new MimeBodyPart();
    try {
      part.setDisposition(ATTACHMENT);
      part.setFileName(attachment.getKey());
      DataHandler dataHandler =
          new DataHandler(new EmailAttachmentDataSource(attachment.getKey(), (InputStream) attachment.getValue().getValue(),
                                                        attachment.getValue().getDataType().getMediaType().toRfcString()));
      part.setDataHandler(dataHandler);
      part.setHeader(CONTENT_TYPE_HEADER, dataHandler.getContentType());
      part.setHeader(CONTENT_TRANSFER_ENCODING_HEADER, attachmentContentTransferEncoding);
      multipart.addBodyPart(part);
    } catch (Exception e) {
      throw new EmailAttachmentException("Error while adding attachment: " + attachment, e);
    }
  }

  /**
   * Converts a simple {@link String} representing an address into an {@link InternetAddress} instance
   *
   * @param address the string to be converted.
   * @return a new {@link InternetAddress} instance.
   */
  private Address toAddress(String address) {
    try {
      return new InternetAddress(address);
    } catch (AddressException e) {
      throw new EmailException(format("Error while creating %s InternetAddress", address));
    }
  }

  /**
   * Converts a {@link List} of {@link String}s representing email addresses into an {@link InternetAddress} array.
   *
   * @param addresses the list to be converted.
   * @return a new {@link Address}[] instance.
   */
  private Address[] toAddressArray(List<String> addresses) {
    return addresses.stream().map(this::toAddress).toArray(Address[]::new);
  }
}
