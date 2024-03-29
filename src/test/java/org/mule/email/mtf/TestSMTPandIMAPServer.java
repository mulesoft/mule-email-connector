/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import com.icegreen.greenmail.imap.ImapHostManager;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.List;
import java.util.Optional;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.DummySSLSocketFactory;

public class TestSMTPandIMAPServer extends AbstractTestServer {

  private static final String JUANI_EMAIL = "juan.desimoni@mulesoft.com";
  private static final String JUANI_PASSWORD = "password";
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final MediaType OCTET_STREAM_UTF8 = MediaType.create("application", "octet-stream", UTF8);
  static final String WEIRD_CHAR_MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

  protected static GreenMail server;
  protected static GreenMailUser user;

  public static void start(Integer imap_port, Integer smtp_port) {
    ServerSetup[] ss = {new ServerSetup(smtp_port, "127.0.0.1", "smtp"),
        new ServerSetup(imap_port, "127.0.0.1", "imap")};
    server = new GreenMail(ss);
    server.start();

    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, JUANI_PASSWORD);
  }

  public static void startSecure(Integer imap_port, Integer smtps_port) {
    Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
    ServerSetup[] ss = {new ServerSetup(smtps_port, "127.0.0.1", "smtps"),
        new ServerSetup(imap_port, "127.0.0.1", "imap")};
    server = new GreenMail(ss);
    server.start();
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, JUANI_PASSWORD);
  }

  public static void stop() {
    server.stop();
  }

  public static void clean() throws FolderException {
    server.purgeEmailFromAllMailboxes();
  }

  public static void passEmailsToIMAPServer() {
    for (MimeMessage message : server.getReceivedMessages()) {
      user.deliver(message);
    }
  }

  public static TypedValue<InputStream> getZipFile() {
    return new TypedValue<>(new ByteArrayInputStream("this is supposedly a zip file".getBytes()),
                            DataType.builder().type(InputStream.class).mediaType(OCTET_STREAM_UTF8).build());
  }

  public static ByteArrayInputStream getContent() {
    return new ByteArrayInputStream("Email Content".getBytes());
  }

  public static String getEncodedBig5Message() throws Exception {
    String defaultEncoding = "UTF-8";
    assertThat(defaultEncoding, is(notNullValue()));
    Optional<String> encoding = availableCharsets().keySet().stream().filter(e -> !e.equals(defaultEncoding)).findFirst();
    assertThat(encoding.isPresent(), is(true));
    return new String(WEIRD_CHAR_MESSAGE.getBytes(UTF8), encoding.get());
  }

  public static void createMailboxes(List<String> mailboxes) throws Exception {
    ImapHostManager imapHostManager = server.getManagers().getImapHostManager();
    for (String mailbox : mailboxes) {
      imapHostManager.createMailbox(user, mailbox);
    }
  }

}
