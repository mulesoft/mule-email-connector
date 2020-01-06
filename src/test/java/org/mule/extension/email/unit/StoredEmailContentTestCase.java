/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.unit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.internal.StoredEmailContentFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class StoredEmailContentTestCase {

  @Test
  public void inputStreamContent() throws IOException, MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message = mockMessage(multipart, "multipart/mixed; boundary=\"f403045e6d18904495056a4ab7e8\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(1));
    TypedValue<InputStream> csv = attachments.get("input.csv");
    assertThat(IOUtils.toString(csv.getValue()), is("orderId,name,units,pricePerUnit\r\n1,aaa,2.0,10\r\n2,bbb,4.15,5"));
  }

  @Test
  public void inputStreamContentFromOutlook() throws IOException, MessagingException {
    InputStream multipart = Thread.currentThread().getContextClassLoader().getResourceAsStream("unit/outlook_multipart");
    StreamingHelper helper = mock(StreamingHelper.class);
    when(helper.resolveCursorProvider(any())).thenAnswer(a -> a.getArgument(0));
    Message message = mockMessage(multipart, "multipart/mixed;\n"
        + "boundary=\"----=_NextPart_000_0039_01D5B1D7.2E5205B0\"");
    StoredEmailContent content = new StoredEmailContentFactory(helper).fromMessage(message);
    Map<String, TypedValue<InputStream>> attachments = content.getAttachments();
    assertThat(attachments.size(), is(2));
    TypedValue<InputStream> image = attachments.get("image001.jpg");
    assertThat(image.getDataType().getMediaType().toString(), is("image/jpeg"));
    assertThat(image.getValue(), not(nullValue()));
  }

  private Message mockMessage(InputStream multipart, String contentType) throws IOException, MessagingException {
    Message message = mock(Message.class);
    when(message.getContent()).thenReturn(multipart);
    when(message.getContentType()).thenReturn(contentType);
    when(message.isMimeType("multipart/*")).thenReturn(true);
    when(message.isMimeType("multipart/alternative")).thenReturn(false);
    when(message.isMimeType("multipart/related")).thenReturn(false);
    when(message.isMimeType("multipart/mixed")).thenReturn(true);
    return message;
  }
}
