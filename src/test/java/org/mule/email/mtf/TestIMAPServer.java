/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf;

import static com.icegreen.greenmail.util.ServerSetup.*;
import static org.mule.extension.email.util.EmailTestUtils.*;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static javax.mail.Flags.Flag.RECENT;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

// TODO: Fix my duplicated code me when MTF can run parameterized tests
public class TestIMAPServer extends AbstractTestServer {

  protected static GreenMail server;
  protected static GreenMailUser user;

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

  public static void setUserWithSpecialPassword() {
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "*uawH*IDXlh2p%21xSPOx%23%25zLpL");
  }

  public static void setUserWithNormalPassword() {
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "password");
  }

  public static void setMailboxRecentFlag(boolean state) throws Exception {
    for (MimeMessage message : server.getReceivedMessages()) {
      message.setFlag(RECENT, state);
    }
  }
}
