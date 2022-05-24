/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.email.api.exception.EmailCountMessagesException;
import org.mule.extension.email.internal.mailbox.MailboxConnection;

import javax.mail.FolderNotFoundException;
import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class POP3OperationsTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static POP3Operations pop3Operations;
  private static MailboxConnection connection;


  @BeforeClass
  public static void setup() {
    pop3Operations = new POP3Operations();
    connection = mock(MailboxConnection.class);
  }

  @Test
  public void countFail() throws MessagingException {
    when(connection.getDefaultFolder()).thenThrow(FolderNotFoundException.class);
    expectedException.expect(EmailCountMessagesException.class);
    expectedException.expectMessage(is("Error while counting messages in the INBOX folder"));
    pop3Operations.countMessagesPop3(connection);
  }

}
