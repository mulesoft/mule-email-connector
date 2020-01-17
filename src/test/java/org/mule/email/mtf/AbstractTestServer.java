/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
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
}
