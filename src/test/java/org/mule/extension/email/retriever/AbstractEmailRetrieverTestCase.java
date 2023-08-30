/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.retriever;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.System.getProperties;
import static javax.mail.Session.getDefaultInstance;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGINATION_OFFSET;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.UNLIMITED;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.testSession;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase {

  final static int DEFAULT_TEST_PAGE_SIZE = valueOf(DEFAULT_PAGE_SIZE);
  static final String RETRIEVE_AND_READ = "retrieveAndRead";
  static final String RETRIEVE_AND_READ_MAX_CONCURRENCY_EQUALS_ONE = "retrieveAndReadMaxConcurrencyEqualsOne";

  private static final String RETRIEVE_AND_DELETE = "retrieveAndDelete";
  private static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
  private static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
  private static final String RETRIEVE_WITH_LIMIT = "retrieveWithLimit";
  private static final String RETRIEVE_WITH_PAGE_SIZE_AND_MATCHER = "retrieveWithPageSize";
  private static final String RETRIEVE_WITH_PAGE_SIZE_AND_OFFSET = "retrieveWithPageSizeAndOffset";
  private static final String RETRIEVE_NUMBERED_WITH_PAGE_SIZE = "retrieveNumberedWithPageSize";

  private static final String TEXT_PLAIN = MediaType.TEXT.toRfcString();
  private static final MediaType TEXT_JSON = MediaType.create("text", "json", Charset.forName("UTF-8"));
  private static final String JSON_OBJECT = "{\"this is a\" : \"json object\"}";
  private static final String BASE64_OBJECT = "b3JkZXJJZCxuYW1lLHVuaXRzLHByaWNlUGVyVW5pdA0KMSxhYWEsMi4wLDEwDQoyLGJiYiw0LjE1LDU=";
  private static final MediaType TEXT_CSV = MediaType.create("text", "csv", Charset.forName("US-ASCII"));

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

  // TODO MULE-16421 : Delete this test once the ticket is solved.
  @Test
  public void retrieveEmailWithAttachments() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMixedTestMessage());
    CoreEvent event = flowRunner(RETRIEVE_WITH_ATTACHMENTS).keepStreamsOpen().run();
    assertThat(getVariableAsString(event, "text"), is(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT));
    assertThat(getVariableAsString(event, "json"), is(EMAIL_JSON_ATTACHMENT_CONTENT));
  }

  // TODO MULE-16421 : Delete this test once the ticket is solved.
  @Test
  public void retrieveAndDelete() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
  @Test
  public void retrieveEmptyPageInBetween() throws Exception {
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE * 2));
  }

  @Test
  public void retrievePagesWithPaginationOffset() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendEmailsWithCounter(10);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSizeAndOffset(RETRIEVE_WITH_PAGE_SIZE_AND_OFFSET, 5, 7);
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    assertThat(paginationSize(messages), is(3));
  }

  @Test
  public void retrievePagesWithSizeOneAndPaginationOffset() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendEmailsWithCounter(10);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSizeAndOffset(RETRIEVE_WITH_PAGE_SIZE_AND_OFFSET, 1, 7);
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    assertThat(paginationSize(messages), is(3));
  }

  @Test
  public void retrievePagesWithFullSizeAndPaginationOffset() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendEmailsWithCounter(10);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSizeAndOffset(RETRIEVE_WITH_PAGE_SIZE_AND_OFFSET, 20, 7);
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    assertThat(paginationSize(messages), is(3));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
  @Test
  public void retrieveHalfPageInBetween() throws Exception {
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithPageSize(RETRIEVE_WITH_PAGE_SIZE_AND_MATCHER, 3);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 3));
    assertThat(paginationSize(messages), is(DEFAULT_TEST_PAGE_SIZE * 2));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
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

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
  @Test
  public void retrieveWithLimitSmallerThanPageSize() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithLimit(RETRIEVE_WITH_LIMIT, 5);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(paginationSize(messages), is(5));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
  @Test
  public void retrieveWithLimitLargerThanPageSize() throws Exception {
    server.purgeEmailFromAllMailboxes();
    sendNonMatchingEmails(DEFAULT_TEST_PAGE_SIZE);
    sendEmails(DEFAULT_TEST_PAGE_SIZE);

    Iterator<Message> messages = runFlowAndGetMessagesWithLimit(RETRIEVE_WITH_LIMIT, 15);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE * 2));
    assertThat(paginationSize(messages), is(15));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
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

  private MimeMessage getMessageFromEmlFile(String file) throws MessagingException {
    InputStream multipart = currentThread().getContextClassLoader().getResourceAsStream(file);
    Properties props = getProperties();
    Session mailSession = getDefaultInstance(props, null);
    return new MimeMessage(mailSession, multipart);
  }

  @Test
  public void retrieveEmailsWithoutBody() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMessageFromEmlFile("unit/only_attachment"));
    Iterator<Message> messageIterator = runFlowAndGetMessages(RETRIEVE_AND_READ_MAX_CONCURRENCY_EQUALS_ONE);
    Message next = messageIterator.next();
    TypedValue<String> body = ((StoredEmailContent) next.getPayload().getValue()).getBody();
    assertThat(body.getValue(), is(""));
    assertThat(((BaseEmailAttributes) next.getAttributes().getValue()).getHeaders().get("Content-Disposition")
        .startsWith("attachment"), is(true));
  }

  // TODO MULE-16388 : Review if it makes sense to migrated this test.
  @Test
  public void retrieveMultiplePagesReadAndDeleteAfter() throws Exception {
    sendEmails(100);
    List<Message> messages = new ArrayList<>();
    runFlowAndGetMessages(RETRIEVE_AND_DELETE).forEachRemaining(messages::add);
    messages.forEach(msg -> assertBodyContent(((StoredEmailContent) msg.getPayload().getValue()).getBody().getValue()));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE + 100));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  @Test
  public void retrieveMultiplePagesReadAndDeleteAfterWithMaxConcurrencyEqualsOne() throws Exception {
    sendEmails(100);
    List<Message> messages = new ArrayList<>();
    runFlowAndGetMessages(RETRIEVE_AND_READ_MAX_CONCURRENCY_EQUALS_ONE).forEachRemaining(messages::add);
    messages.forEach(msg -> assertBodyContent(((StoredEmailContent) msg.getPayload().getValue()).getBody().getValue()));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE + 100));
  }

  private Iterator<Message> runFlowAndGetMessagesWithPageSize(String flowName, int pageSize) throws Exception {
    return runFlowAndGetMessages(flowName, valueOf(UNLIMITED), pageSize);
  }

  private Iterator<Message> runFlowAndGetMessagesWithPageSizeAndOffset(String flowName, int pageSize, int paginationOffset)
      throws Exception {
    return runFlowAndGetMessages(flowName, valueOf(UNLIMITED), pageSize, paginationOffset);
  }

  private Iterator<Message> runFlowAndGetMessagesWithLimit(String flowName, int limit) throws Exception {
    return runFlowAndGetMessages(flowName, limit, valueOf(DEFAULT_PAGE_SIZE));
  }

  private Iterator<Message> runFlowAndGetMessages(String flowName, int limit, int pageSize) throws Exception {
    return runFlowAndGetMessages(flowName, limit, pageSize, Integer.valueOf(DEFAULT_PAGINATION_OFFSET));
  }

  private Iterator<Message> runFlowAndGetMessages(String flowName, int limit, int pageSize, int paginationOffset)
      throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).withVariable("limit", limit).withVariable("pageSize", pageSize)
            .withVariable("paginationOffset", paginationOffset)
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

  private void sendEmailsWithCounter(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, EMAIL_SUBJECT + " counter:" + i,
                                  ESTEBAN_EMAIL));
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

  private String getVariableAsString(CoreEvent event, String text) {
    Object value = event.getVariables().get(text).getValue();
    if (value instanceof CursorStreamProvider) {
      value = org.mule.runtime.core.api.util.IOUtils.toString((CursorStreamProvider) value);
    }
    if (value instanceof InputStream) {
      value = org.mule.runtime.core.api.util.IOUtils.toString((InputStream) value);
    }
    return value.toString();
  }
}
