/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.extension.email.api.IncomingEmail;
import org.mule.runtime.api.message.Message;
import org.mule.test.runner.RunnerDelegateTo;
import org.junit.Test;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunnerDelegateTo(Parameterized.class)
public class POP3TestCase extends AbstractEmailRetrieverTestCase {

  @Parameterized.Parameter
  public String protocol = "pop3";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"pop3"}, {"pop3s"}});
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {format("retriever/%s.xml", protocol), "retriever/pop3-flows.xml"};
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Test
  public void retrieveAndRead() throws Exception {
    List<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    messages.forEach(msg -> assertBodyContent(((IncomingEmail) msg.getPayload().getValue())));
    assertThat(messages, hasSize(DEFAULT_TEST_PAGE_SIZE));
  }
}
