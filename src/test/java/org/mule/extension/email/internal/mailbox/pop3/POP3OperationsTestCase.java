package org.mule.extension.email.internal.mailbox.pop3;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.email.api.exception.EmailCountMessagesException;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;

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
