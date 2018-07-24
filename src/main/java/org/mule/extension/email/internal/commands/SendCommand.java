/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.extension.email.api.EmailError.AUTHENTICATION;
import static org.mule.extension.email.api.EmailError.CONNECTIVITY;
import static org.mule.extension.email.internal.util.EmailUtils.getMediaType;

import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.api.exception.EmailSendException;
import org.mule.extension.email.internal.MessageBuilder;
import org.mule.extension.email.internal.sender.EmailBody;
import org.mule.extension.email.internal.sender.EmailSettings;
import org.mule.extension.email.internal.sender.SMTPConfiguration;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.extension.email.internal.util.AttachmentsGroup;
import org.mule.runtime.api.metadata.MediaType;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.Transport;

import java.util.Calendar;

import com.sun.mail.smtp.SMTPSendFailedException;
import com.sun.mail.util.MailConnectException;

/**
 * Represents the send operation.
 *
 * @since 1.0
 */
public final class SendCommand {

  /**
   * Send an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the {@code settings}.
   *
   * @param connection        the connection associated to the operation.
   * @param smtpConfiguration the specified {@link SMTPConfiguration} to send the email.
   * @param settings          the email settings used to create the email that is going to be sent.
   * @param body              the body part of an email
   * @param attachments       email attachments to send
   */
  public void send(SenderConnection connection, SMTPConfiguration smtpConfiguration,
                   EmailSettings settings, EmailBody body, AttachmentsGroup attachments)
      throws EmailConnectionException {
    try {
      MediaType contentType = getMediaType(body, smtpConfiguration.getDefaultEncoding());
      Message message = MessageBuilder.newMessage(connection.getSession())
          .withSentDate(Calendar.getInstance().getTime())
          .fromAddresses(isNotBlank(settings.getFromAddress()) ? settings.getFromAddress() : smtpConfiguration.getFrom())
          .to(settings.getToAddresses())
          .cc(settings.getCcAddresses())
          .bcc(settings.getBccAddresses())
          .withSubject(settings.getSubject())
          .replyTo(settings.getReplyToAddresses())
          .withAttachments(attachments.getAttachments(), attachments.getContentTransferEncoding())
          .withBody(body.getContentAsString(contentType.getCharset().get()), contentType, body.getContentTransferEncoding())
          .withHeaders(settings.getHeaders())
          .build();
      Transport.send(message);
    } catch (SMTPSendFailedException e) {
      if (isConnectionError(e.getReturnCode())) {
        throw new EmailConnectionException(getSendErrorMessage(e), e, CONNECTIVITY);
      }
      throw new EmailSendException(getSendErrorMessage(e), e);
    } catch (AuthenticationFailedException e) {
      throw new EmailConnectionException(getSendErrorMessage(e), e, AUTHENTICATION);
    } catch (MailConnectException e) {
      throw new EmailConnectionException(getSendErrorMessage(e), e, CONNECTIVITY);
    } catch (Exception e) {
      throw new EmailSendException(getSendErrorMessage(e), e);
    }
  }

  private String getSendErrorMessage(Exception e) {
    return "Error while sending email: " + e.getMessage();
  }

  private boolean isConnectionError(int smtpErrorCode) {
    switch (smtpErrorCode) {
      //Known status codes related to connectivity issues
      case 101 | 110 | 111 | 420 | 421 | 441 | 442 | 447 | 530: {
        return true;
      }
      default:
        return false;
    }
  }
}
