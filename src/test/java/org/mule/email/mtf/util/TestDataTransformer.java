/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.email.mtf.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import javax.activation.DataHandler;

public class TestDataTransformer {

  public TestDataTransformer() {}

  private static final String EMPTY_BODY = "";

  public HashMap<String, Object> extractContent(MimeMessage mimeMessage) {
    HashMap<String, Object> emailContents = new HashMap<>();
    HashMap<String, Object> fileProps = new HashMap<>();

    try {
      if (mimeMessage.getContent() != null && mimeMessage.getContent() instanceof MimeMultipart) {
        MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
          BodyPart bodyPart = mimeMultipart.getBodyPart(i);
          if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
            fileProps.put(bodyPart.getFileName(), new DataHandler(bodyPart.getDataHandler().getDataSource()));
            emailContents.put("Attachment", fileProps);
          } else {
            emailContents.put("Body", bodyPart.getContent());
          }
        }
      } else if (mimeMessage.getContent() != null) {
        fileProps.put(mimeMessage.getFileName(), new DataHandler(mimeMessage.getDataHandler().getDataSource()));
        emailContents.put("Attachment", fileProps);
        emailContents.put("Body", EMPTY_BODY);
        System.out.println("emailContents: " + emailContents);
      } else if (mimeMessage.getContent() != null && mimeMessage.getContent() instanceof String) {
        emailContents.put("Body", mimeMessage.getContent());
      } else {
        emailContents.put("Body", EMPTY_BODY);
      }
    } catch (Exception e) {
      System.out.println(e.toString());
      emailContents.put("Body", EMPTY_BODY);
    }

    return emailContents;
  }

  public MimeMessage transformMessage(String message) {
    MimeMessage mimeMessage = null;

    try {
      Session session = Session.getDefaultInstance(new Properties());

      if (message != null) {
        ByteArrayInputStream bais = new ByteArrayInputStream(message.getBytes());
        mimeMessage = new MimeMessage(session, bais);
      }
    } catch (Exception e) {
      mimeMessage = null;
    }

    return mimeMessage;
  }
}
