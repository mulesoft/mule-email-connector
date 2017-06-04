/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.internal.MessageBuilder.newMessage;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.MG_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.PABLON_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.testSession;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

public class MessageBuilderTestCase {

  private static final String JUANI_NAME = "Juan Desimoni";
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final String BIT7 = "7bit";

  @Test
  public void buildSimpleMessage() throws Exception {
    Message m = newMessage(testSession)
        .withBody(EMAIL_CONTENT)
        .withSubject(EMAIL_SUBJECT)
        .to(asList(ESTEBAN_EMAIL, ALE_EMAIL))
        .cc(asList(PABLON_EMAIL, MG_EMAIL))
        .bcc(singletonList(JUANI_EMAIL))
        .build();

    assertThat(m.getSubject(), is(EMAIL_SUBJECT));
    assertThat(m.getContent(), is(EMAIL_CONTENT));
    assertRecipients(m, TO, ESTEBAN_EMAIL, ALE_EMAIL);
    assertRecipients(m, CC, PABLON_EMAIL, MG_EMAIL);
    assertRecipients(m, BCC, JUANI_EMAIL);
  }


  @Test
  public void buildAttachmentMessage() throws Exception {
    String html = "<h1>Mulesoft</h1>";
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("attachment.json");
    Map<String, TypedValue<InputStream>> attachments = singletonMap(EMAIL_JSON_ATTACHMENT_NAME,
                                                                    new TypedValue<>(is, DataType.builder().fromObject(is)
                                                                        .mediaType(BINARY).build()));

    Message m = newMessage(testSession)
        .withBody(html, HTML.withCharset(UTF8), BIT7)
        .withSubject(EMAIL_SUBJECT)
        .to(asList(ESTEBAN_EMAIL, ALE_EMAIL))
        .withAttachments(attachments, BIT7)
        .bcc(singletonList(JUANI_EMAIL))
        .build();

    assertThat(m.getSubject(), is(EMAIL_SUBJECT));
    assertRecipients(m, TO, ESTEBAN_EMAIL, ALE_EMAIL);
    assertRecipients(m, BCC, JUANI_EMAIL);

    Multipart multipart = (Multipart) m.getDataHandler().getContent();
    Part body = multipart.getBodyPart(0);
    assertThat(body.getDataHandler().getContent(), is(html));
    assertThat(body.getContentType(), is("text/html; charset=UTF-8"));
    assertThat(body.getDataHandler().getContentType(), is("text/html; charset=UTF-8"));

    Part attachmentPart = multipart.getBodyPart(1);
    assertThat(IOUtils.toString((InputStream) attachmentPart.getContent()), is(EMAIL_JSON_ATTACHMENT_CONTENT));
    assertThat(attachmentPart.getContentType(), is(BINARY.toString()));
    assertThat(attachmentPart.getDataHandler().getContentType(), is(BINARY.toString()));
  }

  private void assertRecipients(Message m, Message.RecipientType type, String... emails) throws Exception {
    Stream.of(m.getRecipients(type)).forEach(a -> assertThat(a.toString(), isIn(emails)));
  }

  @Test
  public void stringToAddress() throws MessagingException {
    Message m = newMessage(testSession)
        .withBody(EMAIL_CONTENT)
        .to(singletonList(JUANI_EMAIL))
        .build();

    Address address = m.getRecipients(TO)[0];
    assertAddress(address, JUANI_EMAIL, null);
  }

  @Test
  public void stringToNameAddress() throws MessagingException {
    String nameAddress = getNameAddress(); // address in the "name<address>" format.
    Message m = newMessage(testSession)
        .withBody(EMAIL_CONTENT)
        .to(singletonList(nameAddress))
        .build();
    Address address = m.getRecipients(TO)[0];
    assertAddress(address, JUANI_EMAIL, JUANI_NAME);
  }

  private void assertAddress(Address address, String addressValue, String personal) {
    assertThat(address, is(not(nullValue())));
    assertThat(address, instanceOf(InternetAddress.class));
    assertThat(address.getType(), is("rfc822"));
    assertThat(((InternetAddress) address).getAddress(), is(addressValue));
    assertThat(((InternetAddress) address).getPersonal(), is(personal));
  }

  private String getNameAddress() {
    return format("%s<%s>", JUANI_NAME, JUANI_EMAIL);
  }
}
