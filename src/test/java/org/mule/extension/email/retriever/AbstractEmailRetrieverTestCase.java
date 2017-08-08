/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.UNLIMITED;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.testSession;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.internal.util.StoredEmailContent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.rule.SystemProperty;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase {

  final static int DEFAULT_TEST_PAGE_SIZE = valueOf(DEFAULT_PAGE_SIZE);
  static final String RETRIEVE_AND_READ = "retrieveAndRead";
  static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";

  private static final String RETRIEVE_AND_DELETE = "retrieveAndDelete";
  private static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
  private static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
  private static final String RETRIEVE_WITH_LIMIT = "retrieveWithLimit";
  private static final String RETRIEVE_WITH_PAGE_SIZE_AND_MATCHER = "retrieveWithPageSize";
  private static final String RETRIEVE_NUMBERED_WITH_PAGE_SIZE = "retrieveNumberedWithPageSize";

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
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(paginationSize(messages), is(0));
  }

  @Test
  public void retrieveMatchingSubjectAndFromAddress() throws Exception {
    for (int i = 0; i < DEFAULT_TEST_PAGE_SIZE; i++) {
      String fromAddress = format("address.%s@enterprise.com", i);
      MimeMessage mimeMessage =
          getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", fromAddress);
      user.deliver(mimeMessage);
    }

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE));
  }

  @Test
  public void retrieveEmailWithAttachments() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMultipartTestMessage());
    Event event = flowRunner(RETRIEVE_WITH_ATTACHMENTS).keepStreamsOpen().run();
    assertThat(event.getVariables().get("text").getValue(), is(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT));
    assertThat(event.getVariables().get("json").getValue(), is(EMAIL_JSON_ATTACHMENT_CONTENT));
  }

  @Test
  public void retrieveAndDelete() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  @Test
  public void retrieveEmptyPageInBetween() throws Exception {
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE * 2));
  }

  @Test
  public void retrieveHalfPageInBetween() throws Exception {
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSize(RETRIEVE_WITH_PAGE_SIZE_AND_MATCHER, 3);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE * 2));
  }

  @Test
  public void retrieveFromNewerToOlder() throws Exception {
    server.purgeEmailFromAllMailboxes();
    int sentEmails = DEFAULT_TEST_PAGE_SIZE * 2;
    sendNumberedEmails(sentEmails);

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    assertThat(server.getReceivedMessages(), arrayWithSize(sentEmails));

    int count = 0;
    while (messages.hasNext()) {
      Message m = messages.next();
      count++;
      assertThat(((BaseEmailAttributes) m.getAttributes().getValue()).getSubject(), is(String.valueOf(sentEmails - count)));
    }

    assertThat(count, is(sentEmails));
    assertThat(server.getReceivedMessages(), is(emptyArray()));

  }

  @Test
  public void retrievePaginatedAndFiltered() throws Exception {
    int sentEmails = DEFAULT_TEST_PAGE_SIZE * 2;
    sendEvenEmails(sentEmails);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSize(RETRIEVE_NUMBERED_WITH_PAGE_SIZE, 3);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    int count = 0;
    while (messages.hasNext()) {
      count += 2;
      Message m = messages.next();
      assertThat(((BaseEmailAttributes) m.getAttributes().getValue()).getSubject(), is(String.valueOf(sentEmails - count)));
    }

    assertThat(count, is(sentEmails));
  }

  @Test
  public void retrieveWithLimitSmallerThanPageSize() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithLimit(RETRIEVE_WITH_LIMIT, 5);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(paginationSize(messages), is(5));
  }


  @Test
  public void retrieveWithLimitLargerThanPageSize() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithLimit(RETRIEVE_WITH_LIMIT, 15);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(paginationSize(messages), is(15));
  }

  @Test
  public void retrieveEmailsContainsContentType() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, JSON_OBJECT, TEXT_JSON.toRfcString(), EMAIL_SUBJECT, ESTEBAN_EMAIL));

    Iterator<Message> messageIterator = runFlowAndGetMessages(RETRIEVE_AND_READ);
    Message next = messageIterator.next();

    TypedValue<String> body = ((StoredEmailContent) next.getPayload().getValue()).getBody();
    assertThat(body.getValue(), is(JSON_OBJECT));
    assertThat(body.getDataType(), is(like(String.class, TEXT_JSON)));
  }

  @Test
  public void retrieveMultiplePagesReadAndDeleteAfter() throws Exception {
    sendEmails(100);
    List<Message> messages = newArrayList(runFlowAndGetMessages(RETRIEVE_AND_DELETE));
    messages.forEach(msg -> assertBodyContent(((StoredEmailContent) msg.getPayload().getValue()).getBody().getValue()));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE + 100));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  private Iterator<Message> runFlowAndGetMessagesWithPageSize(String flowName, int pageSize) throws Exception {
    return runFlowAndGetMessages(flowName, valueOf(UNLIMITED), pageSize);
  }

  private Iterator<Message> runFlowAndGetMessagesWithLimit(String flowName, int limit) throws Exception {
    return runFlowAndGetMessages(flowName, limit, valueOf(DEFAULT_PAGE_SIZE));
  }

  private Iterator<Message> runFlowAndGetMessages(String flowName, int limit, int pageSize) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).withVariable("limit", limit).withVariable("pageSize", pageSize)
            .keepStreamsOpen().run().getMessage().getPayload().getValue();
    return (Iterator<Message>) provider.openCursor();
  }


  Iterator<Message> runFlowAndGetMessages(String flowName) throws Exception {
    return runFlowAndGetMessages(flowName, valueOf(UNLIMITED), valueOf(DEFAULT_PAGE_SIZE));
  }

  private int paginationSize(Iterator<?> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }

  void assertFlag(MimeMessage m, Flag flag, boolean contains) {
    try {
      assertThat(m.getFlags().contains(flag), is(contains));
    } catch (MessagingException e) {
      fail("flag assertion error");
    }
  }

  private void sendEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, EMAIL_SUBJECT, ESTEBAN_EMAIL));
    }
  }

  private void sendNumberedEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, String.valueOf(i), ESTEBAN_EMAIL));
    }
  }

  private void sendEvenEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      String subject = i % 2 == 0 ? String.valueOf(i) : EMAIL_SUBJECT;
      user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, subject, ESTEBAN_EMAIL));
    }
  }

  private void sendNonMatchingEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      String fromAddress = format("address.%s@enterprise.com", i);
      MimeMessage mimeMessage =
          getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", fromAddress);
      user.deliver(mimeMessage);
    }
  }

  private MimeMessage getMimeMessage(String to, String cc, String body, String contentType, String subject, String from)
      throws MessagingException {
    MimeMessage mimeMessage = new MimeMessage(testSession);
    mimeMessage.setRecipient(TO, new InternetAddress(to));
    mimeMessage.setRecipient(CC, new InternetAddress(cc));
    mimeMessage.setContent(body, contentType);
    mimeMessage.setSubject(subject);
    mimeMessage.setFrom(new InternetAddress(from));
    return mimeMessage;
  }

}
