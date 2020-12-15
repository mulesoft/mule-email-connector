/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static org.mule.extension.email.internal.EmailProtocol.SMTP;
import static org.mule.extension.email.internal.EmailProtocol.SMTPS;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;

import org.mule.extension.email.internal.MessageBuilder;
import org.mule.extension.email.internal.oauth2.OAuth2Authenticator;
import org.mule.extension.email.internal.oauth2.OAuth2Authenticator.OAuth2Provider;
import org.mule.extension.email.internal.oauth2.OAuth2Authenticator.Return;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;

import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;

import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPTransport;

/**
 * A {@link ConnectionProvider} that returns instances of smtp based {@link SenderConnection}s.
 *
 * @since 1.0
 */
@Alias("smtp-oauth")
@DisplayName("SMTP OAuth Connection")
@AuthorizationCode(
    authorizationUrl = "to complete",
    accessTokenUrl = "to complete")
public class SMTPOAuthProvider extends AbstractSenderProvider {

  @ParameterGroup(name = CONNECTION)
  private SMTPSConnectionSettings connectionSettings;

  private AuthorizationCodeState state;


  /**
   * {@inheritDoc}
   */
  @Override
  public SenderConnection connect() throws ConnectionException {
    System.out.println(state.getAccessToken());

    //    try {
    //      Security.addProvider(new OAuth2Provider());
    //      Return smtpTransport =
    //          OAuth2Authenticator.connectToSmtp(connectionSettings.getHost(), Integer.parseInt(connectionSettings.getPort()),
    //                                            connectionSettings.getUser(), state.getAccessToken(), true);
    //      SMTPMessage smtpMessage = new SMTPMessage(smtpTransport.getSession());
    //      smtpMessage.setText("hola");
    //      smtpMessage.setFrom(connectionSettings.getUser());
    //      smtpMessage.setRecipient(RecipientType.TO, MessageBuilder.toAddress(connectionSettings.getUser()));
    //      ArrayList<String> strings = new ArrayList<>();
    //      strings.add(connectionSettings.getUser());
    //      smtpTransport.getTransport().sendMessage(smtpMessage, MessageBuilder.toAddressArray(strings));
    //    } catch (Exception e) {
    //      throw new ConnectionException(e);
    //    }



    return new SenderConnection(SMTPS, connectionSettings.getUser(),
                                state.getAccessToken(),
                                connectionSettings.getHost(),
                                connectionSettings.getPort(),
                                getConnectionTimeout(),
                                getReadTimeout(),
                                getWriteTimeout(), getProperties(), connectionSettings.getTlsContextFactory(), false);
  }
}
