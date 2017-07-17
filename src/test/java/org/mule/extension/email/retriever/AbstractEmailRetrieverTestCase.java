/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.testSession;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.IncomingEmail;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.tck.junit4.rule.SystemProperty;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase {

  protected final int DEFAULT_TEST_PAGE_SIZE = Integer.valueOf(DEFAULT_PAGE_SIZE);

  protected static final String RETRIEVE_AND_READ = "retrieveAndRead";
  protected static final String RETRIEVE_AND_DELETE = "retrieveAndDelete";
  protected static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";
  protected static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
  protected static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";

  private static final String TEXT_PLAIN = MediaType.TEXT.toRfcString();
  private static final MediaType TEXT_JSON = MediaType.create("text", "json", Charset.forName("UTF-8"));
  private static final String JSON_OBJECT = "{\"this is a\" : \"json object\"}";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty temporaryFolderProperty = new SystemProperty("storePath", temporaryFolder.getRoot().getAbsolutePath());

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    temporaryFolder.delete();
  }

  @Before
  public void sendInitialEmailBatch() throws MessagingException {
    sendEmails(DEFAULT_TEST_PAGE_SIZE);
  }

  @Test
  public void retrieveNothing() throws Exception {
    server.purgeEmailFromAllMailboxes();
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(messages, hasSize(0));
  }

  @Test
  public void retrieveMatchingSubjectAndFromAddress() throws Exception {
    for (int i = 0; i < DEFAULT_TEST_PAGE_SIZE; i++) {
      String from = format("address.%s@enterprise.com", i);
      MimeMessage message = getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", from);
      user.deliver(message);
    }
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE));
  }

  @Test
  public void retrieveEmailWithAttachments() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMultipartTestMessage());
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_WITH_ATTACHMENTS);

    assertThat(messages, hasSize(1));
    IncomingEmail email = ((IncomingEmail) messages.get(0).getPayload().getValue());

    List<TypedValue<InputStream>> attachments = email.getAttachments();
    assertThat(attachments, hasSize(2));
    assertAttachmentContent(attachments.get(0), EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, null);
    assertAttachmentContent(attachments.get(1), EMAIL_JSON_ATTACHMENT_CONTENT, null);
  }

  @Test
  public void retrieveAndDelete() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  @Test
  public void retrieveEmptyPageInBetween() throws Exception {
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE * 2));
  }

  @Test
  public void retrieveEmailsContainsContentType() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, JSON_OBJECT, TEXT_JSON.toRfcString(), EMAIL_SUBJECT, ESTEBAN_EMAIL));

    List<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(messages, hasSize(1));

    IncomingEmail email = ((IncomingEmail) messages.get(0).getPayload().getValue());
    assertThat(email.getBody().getValue(), is(JSON_OBJECT));
    assertThat(email.getBody().getDataType(), is(like(String.class, TEXT_JSON)));
  }

  @Test
  public void retrieveMultiplePagesReadAndDeleteAfter() throws Exception {
    sendEmails(100);
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    messages.forEach(msg -> assertBodyContent(((IncomingEmail) msg.getPayload().getValue())));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE + 100));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  List<Message> runFlowAndGetMessages(String flowName) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).keepStreamsOpen().run().getMessage().getPayload().getValue();
    return Lists.newArrayList(provider.openCursor());
  }

  void assertFlag(MimeMessage m, Flag flag, boolean contains) {
    try {
      assertThat(m.getFlags().contains(flag), is(contains));
    } catch (MessagingException e) {
      fail("flag assertion error");
    }
  }

  void sendEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, EMAIL_SUBJECT, ESTEBAN_EMAIL));
    }
  }

  private void sendNonMatchingEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      String from = format("address.%s@enterprise.com", i);
      MimeMessage msg = getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", from);
      user.deliver(msg);
    }
  }

  private MimeMessage getMimeMessage(String to, String cc, String body, String contentType, String subject, String from) {
    try {
      MimeMessage mimeMessage = new MimeMessage(testSession);
      mimeMessage.setRecipient(TO, new InternetAddress(to));
      mimeMessage.setRecipient(CC, new InternetAddress(cc));
      mimeMessage.setContent(body, contentType);
      mimeMessage.setSubject(subject);
      mimeMessage.setFrom(new InternetAddress(from));
      return mimeMessage;
    } catch (Exception e) {
      throw new RuntimeException("failed", e);
    }
  }
}
