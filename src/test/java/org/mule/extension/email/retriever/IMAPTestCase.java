/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.internal.errors.EmailError.EMAIL_NOT_FOUND;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;

import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.exception.EmailNotFoundException;
import org.mule.extension.email.internal.util.StoredEmailContent;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import org.mule.test.runner.RunnerDelegateTo;

import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class IMAPTestCase extends AbstractEmailRetrieverTestCase {

  private static final String RETRIEVE_AND_DONT_READ = "retrieveAndDontRead";
  private static final String RETRIEVE_AND_MARK_AS_DELETE = "retrieveAndMarkDelete";
  private static final String RETRIEVE_AND_MARK_AS_READ = "retrieveAndMarkRead";
  private static final String RETRIEVE_MATCH_NOT_READ = "retrieveOnlyNotReadEmails";
  private static final String RETRIEVE_AND_DELETE_INCOMING_AND_SCHEDULED = "retrieveAndDeleteIncomingAndScheduled";
  private static final String RETRIEVE_MATCH_RECENT = "retrieveOnlyRecentEmails";
  private static final String FAIL_MARKING_FLAG = "failMarkingEmail";
  private static final String RETRIEVE_DELETE_SELECTED = "retrieveAndDeleteSelected";

  private TestConnectivityUtils connectivityUtils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  private static final String SPECIAL_CHARACTER_PASSWORD = "*uawH*IDXlh2p%21xSPOx%23%25zLpL";

  @Rule
  public SystemProperty specialCharacterPassword = new SystemProperty("specialCharacterPassword", SPECIAL_CHARACTER_PASSWORD);

  @Parameterized.Parameter
  public String protocol = "imap";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"imap"}, {"imaps"}});
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {format("retriever/%s.xml", protocol), "retriever/imap-flows.xml"};
  }


  @Override
  public String getProtocol() {
    return protocol;
  }

  @Test
  public void specialCharacterPassword() {
    connectivityUtils = new TestConnectivityUtils(registry);
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, SPECIAL_CHARACTER_PASSWORD);
    connectivityUtils.assertSuccessConnection("configSpecialCharacterCredentials");
  }


  @Test
  public void retrieveAndRead() throws Exception {
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    int size = 0;
    while (messages.hasNext()) {
      size++;
      Message m = messages.next();
      assertBodyContent(((StoredEmailContent) m.getPayload().getValue()).getBody().getValue());
      assertThat(((IMAPEmailAttributes) m.getAttributes().getValue()).getFlags().isSeen(), is(true));
    }

    assertThat(size, is(DEFAULT_TEST_PAGE_SIZE));
  }

  @Test
  public void retrieveAndDontRead() throws Exception {
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_DONT_READ);
    int count = 0;
    while (messages.hasNext()) {
      Message m = messages.next();
      assertThat(((IMAPEmailAttributes) m.getAttributes().getValue()).getFlags().isSeen(), is(false));
      count++;
    }
    assertThat(count, is(DEFAULT_TEST_PAGE_SIZE));
  }

  @Test
  public void retrieveAndThenRead() throws Exception {
    flowRunner(RETRIEVE_AND_MARK_AS_READ).run();
    MimeMessage[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(10));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, SEEN, true));
  }

  @Test
  public void retrieveAndMarkAsDelete() throws Exception {
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, false));
    runFlow(RETRIEVE_AND_MARK_AS_DELETE);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, true));
  }

  @Test
  public void failSettingFlag() throws Exception {
    expectedError.expectError(NAMESPACE, EMAIL_NOT_FOUND.getType(), EmailNotFoundException.class,
                              "No email was found with id: [0]");
    runFlow(FAIL_MARKING_FLAG);
  }

  @Test
  public void retrieveAndDeleteIncomingAndScheduled() throws Exception {
    MimeMessage[] startMessageBatch = server.getReceivedMessages();
    assertThat(startMessageBatch, arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    // Scheduled for deletion
    startMessageBatch[0].setFlag(DELETED, true);
    runFlow(RETRIEVE_AND_DELETE_INCOMING_AND_SCHEDULED);
    assertThat(server.getReceivedMessages(), arrayWithSize(8));
  }

  @Test
  public void retrieveOnlyNotRead() throws Exception {
    testMatcherFlag(RETRIEVE_MATCH_NOT_READ, SEEN, true);
  }

  @Test
  public void retrieveOnlyRecent() throws Exception {
    testMatcherFlag(RETRIEVE_MATCH_RECENT, RECENT, false);
  }

  @Test
  public void retrieveAndExpungeDelete() throws Exception {
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, false));
    runFlow(RETRIEVE_AND_THEN_EXPUNGE_DELETE);
    assertThat(server.getReceivedMessages().length, is(0));
  }

  @Test
  public void retrieveAndDeleteSelectedEmails() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    runFlow(RETRIEVE_DELETE_SELECTED);
    assertThat(server.getReceivedMessages(), arrayWithSize(5));
  }

  private void testMatcherFlag(String flowName, Flag flag, boolean flagState) throws Exception {
    for (int i = 0; i < 3; i++) {
      MimeMessage message = server.getReceivedMessages()[i];
      message.setFlag(flag, flagState);
    }

    Iterator<Message> messages = runFlowAndGetMessages(flowName);
    assertThat(server.getReceivedMessages(), arrayWithSize(DEFAULT_TEST_PAGE_SIZE));
    int count = 0;
    while (messages.hasNext()) {
      messages.next();
      count++;
    }
    assertThat(count, is(7));
  }

}
