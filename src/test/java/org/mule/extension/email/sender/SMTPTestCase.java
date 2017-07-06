/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public abstract class SMTPTestCase extends EmailConnectorTestCase {

  static final String WEIRD_CHAR_MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

  @Parameter
  public String protocol;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"smtp"}, {"smtps"}});
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {format("sender/%s.xml", protocol), "sender/smtp-flows.xml", "sender/smtp-attachment-flows.xml"};
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  javax.mail.Message[] getReceivedMessagesAndAssertCount(int receivedNumber) {
    assertThat(server.waitForIncomingEmail(5000, receivedNumber), is(true));
    javax.mail.Message[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(receivedNumber));
    return messages;
  }
}
