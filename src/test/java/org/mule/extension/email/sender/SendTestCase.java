/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class SendTestCase extends SMTPTestCase {

  private static final String JSON_VALUE = "{\n  \"key\": \"value\"\n}";
  private static final String ZIP = "this is supposedly a zip file";
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final MediaType OCTET_STREAM_UTF8 = MediaType.create("application", "octet-stream", UTF8);
  private static final MediaType JSON_UTF8 = MediaType.create("application", "json", UTF8);
  private static final MediaType TEXT_PLAIN = MediaType.create("text", "plain", UTF8);
  private static final MediaType MULTIPART_MIXED = MediaType.create("multipart", "mixed");

  // TODO MULE-16413 : migrate remaining tests once the Attachment bugs are fixed.
  @Test
  public void sendEmailWithLargePayloadsBase64() throws Exception {
    String random = RandomStringUtils.random(100);
    byte[] bytes = random.getBytes(UTF8);
    Map<String, BodyPart> bodyParts = sendAttachments("Base64", bytes);
    assertThat(getByteArray(bodyParts, "image.jpg"), is(bytes));
    assertThat(getByteArray(bodyParts, "text.txt"), is(bytes));
    assertThat(getByteArray(bodyParts, "zip.zip"), is(bytes));
    assertThat(getByteArray(bodyParts, null), is(bytes));
  }

  @Test
  public void sendEmailWithLargePayloads7BitGetsCorrupted() throws Exception {
    String random = RandomStringUtils.random(100);
    byte[] bytes = random.getBytes(UTF8);
    Map<String, BodyPart> bodyParts = sendAttachments("7BIT", bytes);
    assertThat(getByteArray(bodyParts, "image.jpg"), is(not(bytes)));
    assertThat(getByteArray(bodyParts, "text.txt"), is(not(bytes)));
    assertThat(getByteArray(bodyParts, "zip.zip"), is(not(bytes)));
    assertThat(getByteArray(bodyParts, null), is(not(bytes)));
  }

  @Test
  public void sendInputStreamEmail() throws Exception {
    flowRunner("sendStreamEmail").withVariable("stream", new ByteArrayInputStream(EMAIL_CONTENT.getBytes())).run();
    assertSingleMail();
  }

  @Test
  public void sendZipFile() throws Exception {
    flowRunner("sendZipFile")
        .withVariable("zipFile", new ByteArrayInputStream("this is supposedly a zip file".getBytes()),
                      DataType.builder().type(InputStream.class).mediaType(OCTET_STREAM_UTF8).build())
        .run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message message = messages[0];
    assertMessage(message, MULTIPART_MIXED);
    MimeMultipart content = (MimeMultipart) message.getContent();
    assertMessage(content.getBodyPart(0), "Email Content", TEXT_PLAIN);
    assertMessage(content.getBodyPart(1), ZIP, OCTET_STREAM_UTF8);
  }

  @Test
  public void sendEmailWithAttachment() throws Exception {
    flowRunner("sendEmailWithAttachment").run();
    Message[] messages = getReceivedMessagesAndAssertCount(4);
    for (Message message : messages) {
      Multipart content = (Multipart) message.getContent();
      int count = content.getCount();
      assertThat(count, is(4));

      Map<String, BodyPart> bodyParts = new HashMap<>();
      for (int i = 0; i < count; i++) {
        BodyPart bodyPart = content.getBodyPart(i);
        bodyParts.put(bodyPart.getFileName(), bodyPart);
      }

      Object body = content.getBodyPart(0).getContent();
      assertBodyContent((String) body);

      assertMessage(bodyParts.get("json-attachment"), JSON_VALUE, JSON_UTF8);
      assertMessage(bodyParts.get("text-attachment"), EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT_PLAIN);
      assertMessage(bodyParts.get("stream-attachment"), EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, OCTET_STREAM_UTF8);
    }
  }

  @Test
  public void sendEmailAttachmentWithNoContentType() throws Exception {
    flowRunner("noContentTypeAttachment").run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message message = messages[0];
    Multipart content = (Multipart) message.getContent();
    assertThat(content.getCount(), is(2));
    Object body = content.getBodyPart(0).getContent();
    assertBodyContent((String) body);
    assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(IOUtils.toString(content.getBodyPart(1).getInputStream())));
  }

  // TODO : migrate test once MULE-16302 is resolved.
  @Test
  public void sendEmailWithoutBody() throws Exception {
    flowRunner("sendEmailWithoutBody").run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertThat(IOUtils.toString(sentMessage.getInputStream()), isEmptyString());
  }

  private void assertSingleMail() throws IOException, jakarta.mail.MessagingException {
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(sentMessage.getContent().toString().trim());
  }

  private static Map<String, BodyPart> getBodyParts(MimeMultipart content) throws MessagingException {
    Map<String, BodyPart> bodyParts = new HashMap<>();
    for (int i = 0; i < content.getCount(); i++) {
      BodyPart bodyPart = content.getBodyPart(i);
      bodyParts.put(bodyPart.getFileName(), bodyPart);
    }
    return bodyParts;
  }

  /**
   * @return a 5000 character one liner string, useful to test restrictions with 7bit, 8bit and binary
   * transfer encodings.
   */
  private String getLargeString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < 5000; i++) {
      stringBuilder.append("abcde12345");
    }
    return stringBuilder.toString();
  }

  private byte[] getByteArray(Map<String, BodyPart> bodyParts, String partName) throws IOException, MessagingException {
    return IOUtils.toByteArray(bodyParts.get(partName).getInputStream());
  }

  private Map<String, BodyPart> sendAttachments(String contentTransferEncoding, byte[] bytes) throws Exception {
    String text = new String(bytes, UTF8);
    flowRunner("sendEmailWithLargePayloads")
        .withVariable("contentTransferEncoding", contentTransferEncoding)
        .withVariable("jpg", bytes, DataType.builder().type(byte[].class).mediaType("image/jpeg").build())
        .withVariable("text", text, DataType.builder().type(String.class).mediaType(TEXT_PLAIN).build())
        .withVariable("zip", new ByteArrayInputStream(bytes),
                      DataType.builder().type(InputStream.class).mediaType(OCTET_STREAM_UTF8).build())
        .withPayload(text).withMediaType(MediaType.parse("text/html").withCharset(UTF8))
        .run();

    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message message = messages[0];
    MimeMultipart content = (MimeMultipart) message.getContent();
    return getBodyParts(content);
  }
}
