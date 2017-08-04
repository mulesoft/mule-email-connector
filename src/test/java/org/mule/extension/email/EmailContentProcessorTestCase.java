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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getSinglePartTestMessage;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.extension.email.internal.util.StoredEmailContent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class EmailContentProcessorTestCase extends AbstractMuleTestCase {

  private StreamingHelper helper;

  @Before
  public void before() {
    helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(anyObject()))
        .then(inv -> new ByteArrayBasedCursorStreamProvider(IOUtils.toByteArray(((InputStream) inv.getArguments()[0]))));
  }

  @Test
  public void emailTextBodyFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    String body = new StoredEmailContent(message, helper).getBody().getValue();
    assertThat(body, is(EMAIL_CONTENT));
  }

  @Test
  public void emailTextBodyFromSinglePart() throws Exception {
    javax.mail.Message message = getSinglePartTestMessage();
    TypedValue<String> body = new StoredEmailContent(message, helper).getBody();
    assertThat(body.getValue(), is(EMAIL_CONTENT));
    assertThat(body.getDataType(), is(like(String.class, TEXT)));
  }

  @Test
  public void emailAttachmentsFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    Map<String, TypedValue<InputStream>> attachments = new StoredEmailContent(message, helper).getAttachments();
    assertThat(attachments.entrySet(), hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
  }

  private void assertAttachmentContent(Map<String, TypedValue<InputStream>> attachments, String name, String expected)
      throws IOException {
    TypedValue attachment = attachments.get(name);
    Object attachmentContent = attachment.getValue();
    if (attachmentContent instanceof CursorProvider) {
      attachmentContent = ((CursorProvider) attachmentContent).openCursor();
    }
    assertThat(IOUtils.toString((InputStream) attachmentContent), is(expected));
  }
}
