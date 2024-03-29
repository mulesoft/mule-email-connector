/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME;
import static org.mule.extension.email.api.attachment.AttachmentNamingStrategy.NAME_HEADERS_SUBJECT;
import static org.mule.extension.email.internal.StoredEmailContentFactory.DEFAULT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_HTML_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_RELATED_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_RELATED_CONTENT_NORMALIZED;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ANOTHER_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_BASE_64_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_BASE_64_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.getAlternativeRelatedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getAlternativeTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getBadBodyEmail;
import static org.mule.extension.email.util.EmailTestUtils.getMessageRFC822TestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMessageRFC822TestMessageWithRepeatedSubjects;
import static org.mule.extension.email.util.EmailTestUtils.getMixedAlternativeRelatedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedAlternativeTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedRelatedAlternativeTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessageWithRepeatedAttachmentNames;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessageWithUnnamedAttachments;
import static org.mule.extension.email.util.EmailTestUtils.getRelatedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getSimpleHTMLTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getSimpleTextTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedRelatedAlternativeCharsetBinaryTestMessage;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class EmailContentProcessorTestCase extends AbstractMuleTestCase {

  private StreamingHelper helper;
  private StoredEmailContentFactory contentFactory;

  @Before
  public void before() {
    helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any()))
        .then(inv -> new ByteArrayBasedCursorStreamProvider(IOUtils.toByteArray(((InputStream) inv.getArguments()[0]))));

    contentFactory = new StoredEmailContentFactory(helper);
  }

  @Test
  public void textFromSimpleTextMail() throws Exception {
    javax.mail.Message message = getSimpleTextTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT));
    assertThat(body.getDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(body.getDataType().getMediaType().getSubType(), is("plain"));
  }

  @Test
  public void textFromSimpleHTMLMail() throws Exception {
    javax.mail.Message message = getSimpleHTMLTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_HTML_CONTENT));
    assertThat(body.getDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(body.getDataType().getMediaType().getSubType(), is("html"));
  }

  @Test
  public void textFromRelatedMail() throws Exception {
    javax.mail.Message message = getRelatedTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_RELATED_CONTENT_NORMALIZED));
  }

  @Test
  public void attachmentFromRelatedMail() throws Exception {
    javax.mail.Message message = getRelatedTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(1));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
  }

  @Test
  public void textFromAlternativeMail() throws Exception {
    javax.mail.Message message = getAlternativeTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_HTML_CONTENT));
  }

  @Test
  public void textFromAlternativeRelatedMail() throws Exception {
    javax.mail.Message message = getAlternativeRelatedTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_RELATED_CONTENT_NORMALIZED));
  }

  @Test
  public void attachmentFromAlternativeRelatedMail() throws Exception {
    javax.mail.Message message = getAlternativeRelatedTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(1));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
  }

  @Test
  public void textFromMixedMail() throws Exception {
    javax.mail.Message message = getMixedTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT));
    assertThat(body.getDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(body.getDataType().getMediaType().getSubType(), is("plain"));
  }

  @Test
  public void attachmentsFromMixedMail() throws Exception {
    javax.mail.Message message = getMixedTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  @Test
  public void attachmentsFromMixedMailWithRepeatedAttachmentNames() throws Exception {
    javax.mail.Message message = getMixedTestMessageWithRepeatedAttachmentNames();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(3));
    assertAttachmentContent(attachments, "attachment_2.json", EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, "attachment_1.json", EMAIL_JSON_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ANOTHER_ATTACHMENT_CONTENT);
  }

  @Test
  public void attachmentsFromMixedMailWithUnnamedAttachments() throws Exception {
    javax.mail.Message message = getMixedTestMessageWithUnnamedAttachments();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(3));
    assertAttachmentContent(attachments, DEFAULT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, DEFAULT_NAME + "_1", EMAIL_JSON_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, DEFAULT_NAME + "_2", EMAIL_TEXT_PLAIN_ANOTHER_ATTACHMENT_CONTENT);
  }

  @Test
  public void textFromMixedAlternativeMail() throws Exception {
    javax.mail.Message message = getMixedAlternativeTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_HTML_CONTENT));
  }

  @Test
  public void attachmentsFromMixedAlternativeMail() throws Exception {
    javax.mail.Message message = getMixedAlternativeTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  @Test
  public void textFromMixedAlternativeRelatedMail() throws Exception {
    javax.mail.Message message = getMixedAlternativeRelatedTestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_RELATED_CONTENT_NORMALIZED));
  }

  @Test
  public void attachmentsFromMixedAlternativeRelatedMail() throws Exception {
    javax.mail.Message message = getMixedAlternativeRelatedTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  @Test
  public void skipBadlyFormedParts() throws Exception {
    javax.mail.Message message = getBadBodyEmail();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(body.getValue(), is(""));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  @Test
  public void textFromMixedMessageRFC822() throws Exception {
    javax.mail.Message message = getMessageRFC822TestMessage();
    TypedValue<String> body = contentFactory.fromMessage(message, NAME).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT));
  }

  @Test
  public void attachmentFromMixedMessageRFC822() throws Exception {
    javax.mail.Message message = getMessageRFC822TestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME_HEADERS_SUBJECT).getAttachments();
    assertThat(attachments.entrySet(), hasSize(1));
    Object attachmentContent = attachments.get(EMAIL_SUBJECT).getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertTrue(attachmentContent.toString().contains(EMAIL_HTML_CONTENT));
    assertTrue(attachmentContent.toString().contains("Subject: " + EMAIL_SUBJECT));
  }

  @Test
  public void attachmentFromMixedMessageRFC822WithRepeatedSubjects() throws Exception {
    javax.mail.Message message = getMessageRFC822TestMessageWithRepeatedSubjects();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME_HEADERS_SUBJECT).getAttachments();
    assertThat(attachments.entrySet(), hasSize(3));
    Object attachmentContent = attachments.get(EMAIL_SUBJECT + ".Not an extension_2").getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertTrue(attachmentContent.toString().contains(EMAIL_HTML_CONTENT));

    attachmentContent = attachments.get(EMAIL_SUBJECT + ".Not an extension_1").getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertTrue(attachmentContent.toString().contains(EMAIL_CONTENT));

    attachmentContent = attachments.get(EMAIL_SUBJECT + ".Not an extension").getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertTrue(attachmentContent.toString().contains(EMAIL_RELATED_CONTENT));
  }

  @Test
  public void testFromMixedRelatedAlternative() throws Exception {
    javax.mail.Message message = getMixedRelatedAlternativeTestMessage();
    StoredEmailContent email = contentFactory.fromMessage(message, NAME);
    TypedValue<String> body = email.getBody();
    Map<String, TypedValue<InputStream>> attachments = email.getAttachments();
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_RELATED_CONTENT_NORMALIZED));
    assertThat(body.getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_RELATED_CONTENT_NORMALIZED));
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  @Test
  public void attachmentsFromMixedMailWithUnsupportedCharset() throws Exception {
    javax.mail.Message message = getMixedRelatedAlternativeCharsetBinaryTestMessage();
    Map<String, TypedValue<InputStream>> attachments = contentFactory.fromMessage(message, NAME).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_BASE_64_ATTACHMENT_NAME, EMAIL_BASE_64_ATTACHMENT_CONTENT);
  }

  private void assertAttachmentContent(Map<String, TypedValue<InputStream>> attachments, String name, String expected)
      throws IOException {
    TypedValue attachment = attachments.get(name);
    Object attachmentContent = attachment.getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertThat(IOUtils.toString((InputStream) attachmentContent), is(expected));
  }
}
