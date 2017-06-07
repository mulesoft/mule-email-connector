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
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.testSession;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase {

  protected static final String RETRIEVE_AND_READ = "retrieveAndRead";
  protected static final String RETRIEVE_AND_DELETE = "retrieveAndDelete";
  protected static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";
  protected static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
  protected static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
  protected static final String STORE_MESSAGES = "storeMessages";
  protected static final String STORE_SINGLE_MESSAGE = "storeSingleMessage";
  private static final String TEXT_PLAIN = MediaType.TEXT.toRfcString();
  private static final MediaType TEXT_JSON = MediaType.create("text", "json", Charset.forName("UTF-8"));
  private static final String JSON_OBJECT = "{\"this is a\" : \"json object\"}";
  protected final int pageSize = Integer.valueOf(DEFAULT_PAGE_SIZE);

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
    sendEmails(pageSize);
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
    for (int i = 0; i < pageSize; i++) {
      String fromAddress = format("address.%s@enterprise.com", i);
      MimeMessage mimeMessage =
          getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", fromAddress);
      user.deliver(mimeMessage);
    }

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize * 2));
    assertThat(paginationSize(messages), is(pageSize));
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

  @Test
  public void retrieveEmailWithAttachments() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMultipartTestMessage());
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_WITH_ATTACHMENTS);

    Message message = messages.next();
    assertThat(messages.hasNext(), is(false));
    assertThat(message.getPayload().getValue(), instanceOf(MultiPartPayload.class));
    List<Message> emailAttachments = ((MultiPartPayload) message.getPayload().getValue()).getParts();

    assertThat(emailAttachments, hasSize(3));
    assertThat(((DefaultMultiPartPayload) message.getPayload().getValue()).hasBodyPart(), is(true));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPartNames(),
               hasItems(EMAIL_JSON_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME));
    assertAttachmentContent(emailAttachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT.getBytes());
    assertAttachmentContent(emailAttachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes());
  }

  @Test
  public void retrieveAndDelete() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize));
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DELETE);
    assertThat(paginationSize(messages), is(pageSize));
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  @Test
  public void retrieveEmptyPageInBetween() throws Exception {
    sendNonMatchingEmails(pageSize);
    sendEmails(pageSize);

    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize * 3));
    assertThat(paginationSize(messages), is(pageSize * 2));
  }

  @Test
  public void retrieveEmailsContainsContentType() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, JSON_OBJECT, TEXT_JSON.toRfcString(), EMAIL_SUBJECT, ESTEBAN_EMAIL));

    Iterator<Message> messageIterator = runFlowAndGetMessages(RETRIEVE_AND_READ);
    Message next = messageIterator.next();

    TypedValue<Object> payload = next.getPayload();
    assertThat(payload.getValue(), is(JSON_OBJECT));
    assertThat(payload.getDataType(), is(like(String.class, TEXT_JSON)));
  }

  protected Iterator<Message> runFlowAndGetMessages(String flowName) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).keepStreamsOpen().run().getMessage().getPayload().getValue();
    return (Iterator<Message>) provider.openCursor();
  }

  protected int paginationSize(Iterator<?> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }

    return count;
  }

  protected void assertFlag(MimeMessage m, Flag flag, boolean contains) {
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

  private void sendNonMatchingEmails(int amount) throws MessagingException {
    for (int i = 0; i < amount; i++) {
      String fromAddress = format("address.%s@enterprise.com", i);
      MimeMessage mimeMessage =
          getMimeMessage(ESTEBAN_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, "Non Matching Subject", fromAddress);
      user.deliver(mimeMessage);
    }
  }

}
