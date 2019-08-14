/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.util;

import static java.lang.Thread.currentThread;
import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.mule.extension.email.internal.StoredEmailContentFactory.DEFAULT_NAME;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TYPE_HEADER;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

import java.net.URL;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.icegreen.greenmail.util.ServerSetup;

public class EmailTestUtils {

  private static final long SERVER_STARTUP_TIMEOUT = 5000;

  public static final String EMAIL_SUBJECT = "Email Subject";
  public static final String EMAIL_CONTENT = "Email Content";
  public static final String EMAIL_HTML_CONTENT = "<html>HTML Content</html>";
  public static final String EMAIL_RELATED_CONTENT = "\"<H1>Hello</H1><a href=\\\"cid:att1\\\">here</a>\"";
  public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT = "This is the email text attachment";
  public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_NAME = "text-attachment";
  public static final String EMAIL_JSON_ATTACHMENT_CONTENT = "{\"key\": \"value\"}";
  public static final String EMAIL_JSON_ATTACHMENT_NAME = "attachment.json";
  public static final String EMAIL_UNNAMED_ATTACHMENT_NAME = DEFAULT_NAME;

  public static final String PABLON_EMAIL = "pablo.musumeci@mulesoft.com";
  public static final String ESTEBAN_EMAIL = "esteban.wasinger@mulesoft.com";
  public static final String JUANI_EMAIL = "juan.desimoni@mulesoft.com";
  public static final String ALE_EMAIL = "ale.g.marra@mulesoft.com";
  public static final String MG_EMAIL = "mariano.gonzalez@mulesoft.com";

  public static final Session testSession = Session.getDefaultInstance(new Properties());

  public static MimeMessage getSimpleTextTestMessage() throws MessagingException {
    MimeMessage message = new MimeMessage(testSession);
    message.setText(EMAIL_CONTENT);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getSimpleHTMLTestMessage() throws MessagingException {
    MimeMessage message = new MimeMessage(testSession);
    message.setContent(EMAIL_HTML_CONTENT, "text/html");
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getRelatedTestMessage() throws Exception {
    MimeMultipart relatedMultipart = new MimeMultipart("related");

    MimeBodyPart htmlBody = new MimeBodyPart();
    htmlBody.setContent(EMAIL_RELATED_CONTENT, "text/html");
    relatedMultipart.addBodyPart(htmlBody);

    MimeBodyPart binaryInlineAttachment = new MimeBodyPart();
    DataSource dataSrc = new ByteArrayDataSource(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes(), BINARY.toString());
    binaryInlineAttachment.setDataHandler(new DataHandler(dataSrc));
    binaryInlineAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    binaryInlineAttachment.setDisposition(INLINE);
    binaryInlineAttachment.setContentID("att1");
    binaryInlineAttachment.addHeader(CONTENT_TYPE_HEADER, BINARY.toString());
    relatedMultipart.addBodyPart(binaryInlineAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(relatedMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getAlternativeTestMessage() throws MessagingException {
    MimeMessage message = new MimeMessage(testSession);
    MimeMultipart multipart = new MimeMultipart("alternative");

    MimeBodyPart text = new MimeBodyPart();
    text.setContent(EMAIL_CONTENT, "text/plain");
    multipart.addBodyPart(text);
    MimeBodyPart html = new MimeBodyPart();
    html.setContent(EMAIL_HTML_CONTENT, "text/html");
    multipart.addBodyPart(html);

    message.setContent(multipart);
    message.setSentDate(new Date());
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getAlternativeRelatedTestMessage() throws MessagingException {
    MimeMultipart alternativeMultipart = new MimeMultipart("alternative");

    MimeBodyPart textPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(textPart);
    textPart.setContent(EMAIL_CONTENT, "text/plain");

    MimeBodyPart htmlPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(htmlPart);
    MimeMultipart relatedMultipart = new MimeMultipart("related");
    htmlPart.setContent(relatedMultipart);

    MimeBodyPart html = new MimeBodyPart();
    html.setContent(EMAIL_RELATED_CONTENT, "text/html");
    relatedMultipart.addBodyPart(html);

    MimeBodyPart binaryInlineAttachment = new MimeBodyPart();
    DataSource dataSrc = new ByteArrayDataSource(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes(), BINARY.toString());
    binaryInlineAttachment.setDataHandler(new DataHandler(dataSrc));
    binaryInlineAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    binaryInlineAttachment.setDisposition(INLINE);
    binaryInlineAttachment.setContentID("att1");
    binaryInlineAttachment.addHeader(CONTENT_TYPE_HEADER, BINARY.toString());
    relatedMultipart.addBodyPart(binaryInlineAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(alternativeMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getMixedTestMessage() throws MessagingException {
    MimeBodyPart body = new MimeBodyPart();
    body.setContent(EMAIL_CONTENT, TEXT.toString());

    MimeBodyPart textAttachment = new MimeBodyPart();
    textAttachment.setDisposition(ATTACHMENT);
    textAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    textAttachment.setContent(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT.toString());

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    Multipart multipart = new MimeMultipart("mixed");
    multipart.addBodyPart(body);
    multipart.addBodyPart(textAttachment);
    multipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(multipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getMixedAlternativeTestMessage() throws MessagingException {
    MimeMultipart mixedMultipart = new MimeMultipart("mixed");

    MimeBodyPart alternativePart = new MimeBodyPart();
    mixedMultipart.addBodyPart(alternativePart);
    MimeMultipart alternativeMultipart = new MimeMultipart("alternative");
    alternativePart.setContent(alternativeMultipart);

    MimeBodyPart textPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(textPart);
    textPart.setContent(EMAIL_CONTENT, "text/plain");

    MimeBodyPart htmlPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(htmlPart);
    htmlPart.setContent(EMAIL_HTML_CONTENT, "text/html");

    MimeBodyPart textAttachment = new MimeBodyPart();
    textAttachment.setDisposition(ATTACHMENT);
    textAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    textAttachment.setContent(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT.toString());

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    mixedMultipart.addBodyPart(textAttachment);
    mixedMultipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(mixedMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getMixedAlternativeRelatedTestMessage() throws MessagingException {
    MimeMultipart mixedMultipart = new MimeMultipart("mixed");

    MimeBodyPart alternativePart = new MimeBodyPart();
    mixedMultipart.addBodyPart(alternativePart);
    MimeMultipart alternativeMultipart = new MimeMultipart("alternative");
    alternativePart.setContent(alternativeMultipart);

    MimeBodyPart textPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(textPart);
    textPart.setContent(EMAIL_CONTENT, "text/plain");

    MimeBodyPart htmlPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(htmlPart);
    MimeMultipart relatedMultipart = new MimeMultipart("related");
    htmlPart.setContent(relatedMultipart);

    MimeBodyPart html = new MimeBodyPart();
    html.setContent(EMAIL_RELATED_CONTENT, "text/html");
    relatedMultipart.addBodyPart(html);

    MimeBodyPart binaryInlineAttachment = new MimeBodyPart();
    DataSource dataSrc = new ByteArrayDataSource(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes(), BINARY.toString());
    binaryInlineAttachment.setDataHandler(new DataHandler(dataSrc));
    binaryInlineAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    binaryInlineAttachment.setDisposition(INLINE);
    binaryInlineAttachment.setContentID("att1");
    binaryInlineAttachment.addHeader(CONTENT_TYPE_HEADER, BINARY.toString());
    relatedMultipart.addBodyPart(binaryInlineAttachment);

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    mixedMultipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(mixedMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getBadBodyEmail() throws MessagingException {
    MimeMultipart mixedMultipart = new MimeMultipart("mixed");

    MimeBodyPart alternativePart = new MimeBodyPart();
    mixedMultipart.addBodyPart(alternativePart);
    MimeMultipart alternativeMultipart = new MimeMultipart("mixed");
    alternativePart.setContent(alternativeMultipart);

    MimeBodyPart textPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(textPart);
    textPart.setContent(EMAIL_CONTENT, "text/plain");

    MimeBodyPart htmlPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(htmlPart);
    htmlPart.setContent(EMAIL_HTML_CONTENT, "text/html");

    MimeBodyPart textAttachment = new MimeBodyPart();
    textAttachment.setDisposition(ATTACHMENT);
    textAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    textAttachment.setContent(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT.toString());

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    mixedMultipart.addBodyPart(textAttachment);
    mixedMultipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(mixedMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getMessageRFC822TestMessage() throws MessagingException {
    MimeMessage messageAsAttachment = new MimeMessage(testSession);
    messageAsAttachment.setContent(EMAIL_HTML_CONTENT, "text/html");
    messageAsAttachment.setSubject(EMAIL_SUBJECT);
    messageAsAttachment.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    messageAsAttachment.saveChanges();

    MimeBodyPart emailAttachment = new MimeBodyPart();
    emailAttachment.setContent(messageAsAttachment, "message/rfc822");

    MimeBodyPart body = new MimeBodyPart();
    body.setContent(EMAIL_CONTENT, TEXT.toString());

    Multipart multipart = new MimeMultipart("mixed");
    multipart.addBodyPart(body);
    multipart.addBodyPart(emailAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(multipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }

  public static MimeMessage getMixedRelatedAlternativeTestMessage() throws MessagingException {
    MimeMultipart mixedMultipart = new MimeMultipart("mixed");

    MimeBodyPart relatedPart = new MimeBodyPart();
    mixedMultipart.addBodyPart(relatedPart);
    MimeMultipart relatedMultipart = new MimeMultipart("related");
    relatedPart.setContent(relatedMultipart);

    MimeBodyPart alternativePart = new MimeBodyPart();
    relatedMultipart.addBodyPart(alternativePart);
    MimeMultipart alternativeMultipart = new MimeMultipart("alternative");
    alternativePart.setContent(alternativeMultipart);

    MimeBodyPart textPart = new MimeBodyPart();
    alternativeMultipart.addBodyPart(textPart);
    textPart.setContent(EMAIL_CONTENT, "text/plain");

    MimeBodyPart html = new MimeBodyPart();
    html.setContent(EMAIL_RELATED_CONTENT, "text/html");
    alternativeMultipart.addBodyPart(html);

    MimeBodyPart binaryInlineAttachment = new MimeBodyPart();
    DataSource dataSrc = new ByteArrayDataSource(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes(), BINARY.toString());
    binaryInlineAttachment.setDataHandler(new DataHandler(dataSrc));
    binaryInlineAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    binaryInlineAttachment.setDisposition(INLINE);
    binaryInlineAttachment.setContentID("att1");
    binaryInlineAttachment.addHeader(CONTENT_TYPE_HEADER, BINARY.toString());
    relatedMultipart.addBodyPart(binaryInlineAttachment);

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    mixedMultipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(mixedMultipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    message.saveChanges();
    return message;
  }


  public static ServerSetup setUpServer(int port, String protocol) {
    ServerSetup serverSetup = new ServerSetup(port, null, protocol);
    serverSetup.setServerStartupTimeout(SERVER_STARTUP_TIMEOUT);
    return serverSetup;
  }
}
