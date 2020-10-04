/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf;

import static java.util.Arrays.stream;
import static com.icegreen.greenmail.util.ServerSetup.PROTOCOL_IMAP;
import static com.icegreen.greenmail.util.ServerSetup.PROTOCOL_IMAPS;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Flags.Flag.ANSWERED;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.getAlternativeTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMessageRFC822TestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMixedTestMessageWithRepeatedAttachmentNames;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import javax.mail.Flags;
import javax.mail.MessagingException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

// TODO: Fix my duplicated code me when MTF can run parameterized tests
public class TestIMAPServer extends AbstractTestServer {

  protected static GreenMail server;
  protected static GreenMailUser user;
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIMAPServer.class);

  public static void start(Integer port) {
    doStart(port, PROTOCOL_IMAP);
  }

  public static void startSecure(Integer port) {
    doStart(port, PROTOCOL_IMAPS);
  }

  private static void doStart(Integer port, String protocol) {
    server = new GreenMail(new ServerSetup(port, null, protocol));
    server.start();
    setUserWithNormalPassword();
  }

  public static void sendEmail() {
    sendEmailWithSubject(EMAIL_SUBJECT, ESTEBAN_EMAIL);
  }

  public static void sendEmailWithSubject(String subject, String from) {
    user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, subject, from));
  }

  public static void replyToAll() {
    stream(server.getReceivedMessages()).forEach(email -> {
      try {
        email.reply(true);
      } catch (MessagingException e) {
        LOGGER.error(e.getMessage());
      }
    });
  }

  public static void markAllAsDeleted() {
    markAllEmailAs(DELETED, true);
  }

  public static void markAllAsSeen() {
    markAllEmailAs(SEEN, true);
  }

  public static void markAllAsRecent() {
    markAllEmailAs(RECENT, true);
  }

  public static void markAllAsNotRecent() {
    markAllEmailAs(RECENT, false);
  }

  public static void markAllAsDeletedWhenSubject(String subject) {
    markEmailAsWhenSubjectContains(subject, DELETED, true);
  }

  public static void markAllAsAnsweredWhenSubject(String subject) {
    markEmailAsWhenSubjectContains(subject, ANSWERED, true);
  }

  public static void markAllAsRecentWhenSubject(String subject) {
    markEmailAsWhenSubjectContains(subject, RECENT, true);
  }

  public static void markAllAsSeenWhenSubject(String subject) {
    markEmailAsWhenSubjectContains(subject, SEEN, true);
  }

  public static void sendEmailWithAllFields(String to, String cc, String body, String contentType, String subject, String from) {
    user.deliver(getMimeMessage(to, cc, body, contentType, subject, from));
  }

  public static void sendMultiPartAlternativeEmail() {
    withContextClassLoader(server.getClass().getClassLoader(), () -> {
      try {
        user.deliver(getAlternativeTestMessage());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void sendRFC822TestMessage() {
    withContextClassLoader(server.getClass().getClassLoader(), () -> {
      try {
        user.deliver(getMessageRFC822TestMessage());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void sendMixedTestMessageWithRepeatedAttachmentNames() {
    withContextClassLoader(server.getClass().getClassLoader(), () -> {
      try {
        user.deliver(getMixedTestMessageWithRepeatedAttachmentNames());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void sendMultiPartTestEmail() {
    withContextClassLoader(server.getClass().getClassLoader(), () -> {
      try {
        user.deliver(getMixedTestMessage());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void stop() {
    server.stop();
  }

  public static void clean() throws FolderException {
    server.purgeEmailFromAllMailboxes();
  }

  public static void closeInboxFolder() throws MessagingException {
    //Store store = testSession.getStore("imap");
    //Folder folder = store.getFolder("INBOX");
    server.getImap().interrupt();
  }

  public static void setUserWithSpecialPassword() {
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "*uawH*IDXlh2p%21xSPOx%23%25zLpL");
  }

  public static void setUserWithNormalPassword() {
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "password");
  }

  public static void setMailboxRecentFlag(boolean state) throws Exception {
    markAllEmailAs(RECENT, state);
  }

  public static void sendEmailWithSubjectAndSentDate(String subject, String from, String sinceDate) {
    user.deliver(getMimeMessage(JUANI_EMAIL, ALE_EMAIL, EMAIL_CONTENT, TEXT_PLAIN, subject, from, sinceDate));
  }

  private static void markAllEmailAs(Flags.Flag flag, boolean set) {
    stream(server.getReceivedMessages()).forEach(email -> {
      try {
        email.setFlag(flag, set);
      } catch (MessagingException e) {
        LOGGER.error(e.getMessage());
      }
    });
  }

  private static void markEmailAsWhenSubjectContains(String subject, Flags.Flag flag, boolean set) {
    stream(server.getReceivedMessages()).forEach(email -> {
      try {
        if (email.getSubject().contains(subject)) {
          email.setFlag(flag, set);
        }
      } catch (MessagingException e) {
        LOGGER.error(e.getMessage());
      }
    });
  }
}