/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getSinglePartTestMessage;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.extension.email.internal.util.StoredEmailContent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.junit.Test;
import java.io.InputStream;
import java.util.Map;

public class EmailContentProcessorTestCase extends AbstractMuleTestCase {

  @Test
  public void emailTextBodyFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    String body = new StoredEmailContent(message).getBody().getValue();
    assertThat(body, is(EMAIL_CONTENT));
  }

  @Test
  public void emailTextBodyFromSinglePart() throws Exception {
    javax.mail.Message message = getSinglePartTestMessage();
    TypedValue<String> body = new StoredEmailContent(message).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT));
    assertThat(body.getDataType(), is(like(String.class, TEXT)));
  }

  @Test
  public void emailAttachmentsFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    Map<String, TypedValue<InputStream>> attachments = new StoredEmailContent(message).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }
}
