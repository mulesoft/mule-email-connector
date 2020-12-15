/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.oauth2;

import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.smtp.SMTPTransport;

import java.security.Provider;
import java.security.Security;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Session;
import javax.mail.URLName;


/**
 * Performs OAuth2 authentication.
 *
 * <p>Before using this class, you must call {@code initialize} to install the
 * OAuth2 SASL provider.
 */
public class OAuth2Authenticator {

  private static final Logger logger =
      Logger.getLogger(OAuth2Authenticator.class.getName());

  public static final class OAuth2Provider extends Provider {

    private static final long serialVersionUID = 1L;

    public OAuth2Provider() {
      super("Google OAuth2 Provider", 1.0,
            "Provides the XOAUTH2 SASL Mechanism");
      put("SaslClientFactory.XOAUTH2",
          "org.mule.extension.email.internal.oauth2.OAuth2SaslClientFactory");
    }
  }

  /**
   * Installs the OAuth2 SASL provider. This must be called exactly once before
   * calling other methods on this class.
   */
  public static void initialize() {
    Security.addProvider(new OAuth2Provider());
  }

  /**
   * Connects and authenticates to an IMAP server with OAuth2. You must have
   * called {@code initialize}.
   *
   * @param host Hostname of the imap server, for example {@code
   *     imap.googlemail.com}.
   * @param port Port of the imap server, for example 993.
   * @param userEmail Email address of the user to authenticate, for example
   *     {@code oauth@gmail.com}.
   * @param oauthToken The user's OAuth token.
   * @param debug Whether to enable debug logging on the IMAP connection.
   *
   * @return An authenticated IMAPStore that can be used for IMAP operations.
   */
  public static IMAPStore connectToImap(String host,
                                        int port,
                                        String userEmail,
                                        String oauthToken,
                                        boolean debug)
      throws Exception {
    Properties props = new Properties();
    props.put("mail.imaps.sasl.enable", "true");
    props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
    props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
    Session session = Session.getInstance(props);
    session.setDebug(debug);

    final URLName unusedUrlName = null;
    IMAPSSLStore store = new IMAPSSLStore(session, unusedUrlName);
    final String emptyPassword = "";
    store.connect(host, port, userEmail, emptyPassword);
    return store;
  }

  /**
   * Connects and authenticates to an SMTP server with OAuth2. You must have
   * called {@code initialize}.
   *
   * @param host Hostname of the smtp server, for example {@code
   *     smtp.googlemail.com}.
   * @param port Port of the smtp server, for example 587.
   * @param userEmail Email address of the user to authenticate, for example
   *     {@code oauth@gmail.com}.
   * @param oauthToken The user's OAuth token.
   * @param debug Whether to enable debug logging on the connection.
   *
   * @return An authenticated SMTPTransport that can be used for SMTP
   *     operations.
   */
  public static Return connectToSmtp(String host,
                                     int port,
                                     String userEmail,
                                     String oauthToken,
                                     boolean debug)
      throws Exception {
    Properties props = new Properties();
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");
    props.put("mail.smtp.sasl.enable", "true");
    props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
    props.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
    Session session = Session.getInstance(props);
    session.setDebug(debug);

    final URLName unusedUrlName = null;
    SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
    // If the password is non-null, SMTP tries to do AUTH LOGIN.
    final String emptyPassword = "";
    transport.connect(host, port, userEmail, emptyPassword);

    return new Return(transport, session);
  }

  public static class Return {

    SMTPTransport transport;
    Session session;

    public Return(SMTPTransport transport, Session session) {
      this.transport = transport;
      this.session = session;
    }

    public SMTPTransport getTransport() {
      return transport;
    }

    public Session getSession() {
      return session;
    }
  }

  /**
   * Authenticates to IMAP with parameters passed in on the commandline.
   */
  public static void main(String args[]) throws Exception {
    if (args.length != 2) {
      System.err.println(
                         "Usage: OAuth2Authenticator <email> <oauthToken>");
      return;
    }
    String email = args[0];
    String oauthToken = args[1];

    initialize();

    IMAPStore imapStore = connectToImap("imap.gmail.com",
                                        993,
                                        email,
                                        oauthToken,
                                        true);
    System.out.println("Successfully authenticated to IMAP.\n");

    System.out.println("Successfully authenticated to SMTP.");
  }
}
