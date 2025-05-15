/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import org.junit.Test;
import org.mule.extension.email.internal.sender.EmailBody;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class EmailBodyTestCase {

  @Test
  public void testGetContentAsString() throws Exception {
    String expectedContent = "This a Mock body Message";
    byte[] contentBytes = expectedContent.getBytes(StandardCharsets.UTF_8);
    InputStream emailBodyStream = new ByteArrayInputStream(contentBytes);
    TypedValue<InputStream> content = new TypedValue<>(emailBodyStream, null);
    EmailBody emailBody = new EmailBody(content, "text/plain", "UTF-8");
    String result = emailBody.getContentAsString(StandardCharsets.UTF_8);
    assertEquals(expectedContent, result);
  }

  @Test
  public void testGetContentRepeatableResultAsString() throws Exception {
    String bodyMessage = "This a Mock body Message";
    byte[] contentBytes = bodyMessage.getBytes(StandardCharsets.UTF_8);
    InputStream emailBodyStream = new ByteArrayInputStream(contentBytes);
    TypedValue<InputStream> content = new TypedValue<>(emailBodyStream, null);
    EmailBody emailBody = new EmailBody(content, "text/plain", "UTF-8");
    String result = emailBody.getContentAsString(StandardCharsets.UTF_8);
    String repeatableResult = emailBody.getContentAsString(StandardCharsets.UTF_8);
    assertEquals(result, repeatableResult);
  }

  @Test
  public void testGetContentNullAsString() throws Exception {
    EmailBody emailBody = new EmailBody(null, "text/plain", "UTF-8");
    String result = emailBody.getContentAsString(StandardCharsets.UTF_8);
    assertEquals("", result);
  }
}
