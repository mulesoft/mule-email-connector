/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.unit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME_HEADERS;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME_HEADERS_SUBJECT;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class StoredEmailContentTestCase {


  private Message getMessageFromEmlFile(String file) throws MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
    Properties props = System.getProperties();
    Session mailSession = Session.getDefaultInstance(props, null);
    return new MimeMessage(mailSession, multipart);
  }

  @Test
  public void inputStreamContent() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> csv = attachments.get("input.csv");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
  }

  @Test
  public void inputStreamContent_HeadersStrategy() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/multipart_no_filename");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> csv = attachments.get("anotherName.csv");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
  }

  @Test
  public void multipartMixed_test_Unnamed() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/multipart_no_name_and_filename");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    assertThat(attachments.get("Unnamed"), not(nullValue()));
  }

  @Test
  public void multipartMixed_test() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/multipart_basic");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    assertThat(content.getBody().getValue(), is("this is the body text"));
  }

  @Test
  public void multipartMixed_withAttachmentsAndWithoutBody() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/mixed_with_attachments_and_without_body");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    assertThat(content.getBody().getValue(), is(""));
  }

  @Test
  public void noMultipart_withAttachmentsAndWithoutBody() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/only_attachment");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    assertThat(attachments.containsKey("test.csv"), is(true));
    TypedValue<InputStream> csv = attachments.get("test.csv");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
    assertThat(content.getBody().getValue(), is(""));
  }

  @Test
  public void noMultipart_withAttachmentsAndWithoutBody02() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/only_attachment_02");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    assertThat(attachments.containsKey("test.xls"), is(true));
    TypedValue<InputStream> csv = attachments.get("test.xls");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
    assertThat(content.getBody().getValue(), is(""));
  }

  @Test
  public void noMultipart_withAttachmentsAndWithoutBody03() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/only_attachment_03");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    assertThat(attachments.containsKey("test.xls"), is(true));
    TypedValue<InputStream> csv = attachments.get("test.xls");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
    assertThat(content.getBody().getValue(), is(""));
  }

  @Test
  public void multipartMixed_withAttachmentsAndBody() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/mixed_with_attachments_and_with_body");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    assertThat(content.getBody().getValue(), is("This is some content!"));
  }

  @Test
  public void inputStreamContent_SubjectStrategy() throws IOException, MessagingException {
    Message message = getMessageFromEmlFile("unit/multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS_SUBJECT);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> csv = attachments.get("input.csv");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
  }

  @Test
  public void inputStreamContentFromOutlook_DefaultStrategy() throws IOException, MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message = mockMessage(multipart, "multipart/mixed;\n"
        + "boundary=\"----=_NextPart_000_0039_01D5B1D7.2E5205B0\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    TypedValue<InputStream> image = attachments.get("Unnamed_1");
    assertThat(image.getDataType().getMediaType().toString(), is("image/jpeg"));
    assertThat(image.getValue(), not(nullValue()));
  }

  @Test
  public void inputStreamContentFromOutlook_HeadersStrategy() throws IOException, MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message = mockMessage(multipart, "multipart/mixed;\n"
        + "boundary=\"----=_NextPart_000_0039_01D5B1D7.2E5205B0\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    TypedValue<InputStream> image = attachments.get("image001.jpg");
    assertThat(image.getDataType().getMediaType().toString(), is("image/jpeg"));
    assertThat(image.getValue(), not(nullValue()));
  }

  @Test
  public void inputStreamContentFromOutlook_SubjectStrategy() throws IOException, MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message = mockMessage(multipart, "multipart/mixed;\n"
        + "boundary=\"----=_NextPart_000_0039_01D5B1D7.2E5205B0\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS_SUBJECT);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    TypedValue<InputStream> image = attachments.get("image001.jpg");
    assertThat(image.getDataType().getMediaType().toString(), is("image/jpeg"));
    assertThat(image.getValue(), not(nullValue()));
  }

  @Test
  public void inputStreamContentFromOutlook_NestedEmailAttachment_Default() throws IOException, MessagingException {
    InputStream multipart =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart_nested_email_attachment");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message =
        mockMessage(multipart, "multipart/mixed; boundary=\"_004_FR1PR80MB4581A7AAC99CFF0C630598C192360FR1PR80MB4581lamp_\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> attachedEmail = attachments.get("Unnamed");
    assertThat(attachedEmail.getDataType().getMediaType().toString(), is("message/rfc822"));
    assertThat(attachedEmail.getValue(), not(nullValue()));
  }

  @Test
  public void inputStreamContentFromOutlook_NestedEmailAttachment_Subject() throws IOException, MessagingException {
    InputStream multipart =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart_nested_email_attachment");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message =
        mockMessage(multipart, "multipart/mixed; boundary=\"_004_FR1PR80MB4581A7AAC99CFF0C630598C192360FR1PR80MB4581lamp_\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message, NAME_HEADERS_SUBJECT);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> attachedEmail = attachments.get("TestEmail");
    assertThat(attachedEmail.getDataType().getMediaType().toString(), is("message/rfc822"));
    assertThat(attachedEmail.getValue(), not(nullValue()));
  }

  private Message mockMessage(InputStream multipart, String contentType) throws IOException, MessagingException {
    Message message = mock(Message.class);
    when(message.getContent()).thenReturn(multipart);
    when(message.getContentType()).thenReturn(contentType);
    when(message.isMimeType("multipart/*")).thenReturn(true);
    when(message.isMimeType("multipart/alternative")).thenReturn(false);
    when(message.isMimeType("multipart/related")).thenReturn(false);
    when(message.isMimeType("multipart/mixed")).thenReturn(true);
    return message;
  }
}
