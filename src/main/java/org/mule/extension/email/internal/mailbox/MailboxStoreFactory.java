/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.internal.mailbox;

import static org.mule.extension.email.api.exception.EmailError.CONNECTION_TIMEOUT;
import static org.mule.extension.email.api.exception.EmailError.INVALID_CREDENTIALS;
import static org.mule.extension.email.api.exception.EmailError.UNKNOWN_HOST;

import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.internal.EmailProtocol;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * A Factory for {@link Store} objects.
 *
 * @since 1.0
 */
class MailboxStoreFactory {

  static Store getStore(Session session, EmailProtocol protocol, String username, String password)
      throws EmailConnectionException {
    try {
      Store store = session.getStore(protocol.getName());
      if (username != null && password != null) {
        store.connect(username, password);
      } else {
        store.connect();
      }
      return store;
    } catch (AuthenticationFailedException e) {
      throw new EmailConnectionException(e, INVALID_CREDENTIALS);
    } catch (MessagingException e) {
      throw handleEmailMessagingException(e);
    } catch (IllegalArgumentException e) {
      throw handleIllegalArgumentException(e);
    } catch (Exception e) {
      throw new EmailConnectionException(e);
    }
  }

  private static EmailConnectionException handleIllegalArgumentException(IllegalArgumentException e) {
    return e.getMessage().contains("port out of range") ? new EmailConnectionException(e, UNKNOWN_HOST)
        : new EmailConnectionException(e);
  }

  private static EmailConnectionException handleEmailMessagingException(MessagingException e) {
    if (e.getCause() instanceof SocketTimeoutException) {
      return new EmailConnectionException(e, CONNECTION_TIMEOUT);
    }
    if (e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException) {
      return new EmailConnectionException(e, UNKNOWN_HOST);
    }
    return new EmailConnectionException(e);
  }

}
