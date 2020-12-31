/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf;

import static java.sql.Date.from;
import static java.time.LocalDateTime.parse;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;

import static java.time.ZoneId.systemDefault;
import static jakarta.mail.Message.RecipientType.CC;
import static jakarta.mail.Message.RecipientType.TO;
import static org.mule.extension.email.util.EmailTestUtils.*;

public class AbstractTestServer {

  protected static MimeMessage getMimeMessage(String to, String cc, String body, String contentType, String subject,
                                              String from) {
    try {
      MimeMessage message = new MimeMessage(testSession);
      message.setRecipient(TO, new InternetAddress(to));
      message.setRecipient(CC, new InternetAddress(cc));
      message.setContent(body, contentType);
      message.setSubject(subject);
      message.setFrom(new InternetAddress(from));
      return message;
    } catch (Exception e) {
      throw new RuntimeException("Cannot create test Email", e);
    }
  }

  protected static MimeMessage getMimeMessage(String to, String cc, String body, String contentType, String subject,
                                              String from, String sentDate) {
    try {
      MimeMessage message = getMimeMessage(to, cc, body, contentType, subject, from);
      message.setSentDate(convertLocalDateTimeStringToDate(sentDate));
      return message;
    } catch (Exception e) {
      throw new RuntimeException("Cannot create test Email", e);
    }
  }

  private static Date convertLocalDateTimeStringToDate(String localDateTime) {
    return from(parse(localDateTime).atZone(systemDefault()).toInstant());
  }


}
