/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.runtime.api.message.Message;
import org.mule.test.runner.RunnerDelegateTo;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

@RunnerDelegateTo(Parameterized.class)
public class POP3TestCase extends AbstractEmailRetrieverTestCase {

  @Parameter
  public String protocol;

  @Parameters(name = "{0}")
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
    Iterator<Message> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    int count = 0;
    while (messages.hasNext()) {
      assertBodyContent(((StoredEmailContent) messages.next().getPayload().getValue()).getBody().getValue());
      count++;
    }
    assertThat(count, is(DEFAULT_TEST_PAGE_SIZE));
  }
}
