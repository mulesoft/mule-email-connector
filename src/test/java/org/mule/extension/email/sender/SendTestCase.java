/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.junit.Test;

public class SendTestCase extends SMTPTestCase {

  @Test
  public void sendEmail() throws Exception {
    flowRunner("sendEmail").run();
    assertSingleMail();
  }

  @Test
  public void sendInputStreamEmail() throws Exception {
    flowRunner("sendStreamEmail").withVariable("stream", new ByteArrayInputStream(EMAIL_CONTENT.getBytes())).run();
    assertSingleMail();
  }

  @Test
  public void sendJson() throws Exception {
    String json = "{\"key\":\"value\"}";
    flowRunner("sendJson").withVariable("json", new ByteArrayInputStream(json.getBytes())).run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    assertThat(IOUtils.toString(messages[0].getInputStream()).trim(), is(json));
  }

  private void assertSingleMail() throws MessagingException, IOException {
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(sentMessage.getContent().toString().trim());
  }

  @Test
  public void sendEmailCustomHeaders() throws Exception {
    flowRunner("sendEmailHeaders").run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(sentMessage.getContent().toString().trim());

    assertThat(sentMessage.getHeader("CustomOperationHeader"), arrayWithSize(1));
    assertThat(sentMessage.getHeader("CustomOperationHeader")[0], is("Dummy"));
  }

  @Test
  public void sendEmailWithAttachment() throws Exception {
    InputStream jsonStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("attachment.json");
    flowRunner("sendEmailWithAttachment").withVariable("jsonStream", jsonStream).run();
    Message[] messages = getReceivedMessagesAndAssertCount(4);
    for (Message message : messages) {
      Multipart content = (Multipart) message.getContent();
      assertThat(content.getCount(), is(4));

      Object body = content.getBodyPart(0).getContent();
      assertBodyContent((String) body);

      String textAttachment = (String) content.getBodyPart(1).getContent();
      assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(textAttachment));

      DataHandler jsonAttachment = content.getBodyPart(2).getDataHandler();
      assertJsonAttachment(jsonAttachment);

      DataHandler streamAttachment = content.getBodyPart(3).getDataHandler();
      assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) streamAttachment.getContent())));
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
    assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) content.getBodyPart(1).getContent())));
  }

  @Test
  public void sendEncodedMessage() throws Exception {
    String defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));
    Optional<String> enconding = availableCharsets().keySet().stream().filter(e -> !e.equals(defaultEncoding)).findFirst();
    assertThat(enconding.isPresent(), is(true));

    flowRunner("sendEncodedMessage").withPayload(WEIRD_CHAR_MESSAGE).withVariable("encoding", enconding.get()).run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Object content = ((String) messages[0].getContent()).trim();
    assertThat(content, is(new String(WEIRD_CHAR_MESSAGE.getBytes(enconding.get()), enconding.get())));
  }

  @Test
  public void sendEmailWithoutBody() throws Exception {
    flowRunner("sendEmailWithoutBody").run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertThat(sentMessage.getContent().toString().trim(), isEmptyString());
  }
}
