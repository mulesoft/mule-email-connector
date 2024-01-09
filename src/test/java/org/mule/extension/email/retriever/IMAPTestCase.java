/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_HTML_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.getAlternativeTestMessage;

import org.mule.extension.email.api.StoredEmailContent;
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

  // TODO MULE-16421 : Delete this test once the ticket is solved.
  @Test
  public void readMultiPartAlternative() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getAlternativeTestMessage());
    Message msg = runFlowAndGetMessages(RETRIEVE_AND_READ).next();
    StoredEmailContent emailContent = (StoredEmailContent) msg.getPayload().getValue();
    assertThat(emailContent.getBody().getValue(), is(EMAIL_CONTENT + "\n" + EMAIL_HTML_CONTENT));
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
