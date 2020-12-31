/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.mail.internet.MimeMessage;

import org.junit.Test;
import org.mule.extension.email.internal.util.DefaultMailPartContentResolver;
import org.mule.extension.email.internal.util.MailPartContentResolver;

import com.sun.mail.imap.IMAPMessage;

public class DefaultMailPartContentResolverTestCase {

  private static final String CONTENT_PAYLOAD = "CONTENT";
  private static final String CONTENT_FILE = "unit/content.txt";

  @Test
  public void resolveIMAPPartContentToByteArrayStream() throws Exception {
    try (InputStream imapInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONTENT_FILE)) {
      MailPartContentResolver resolver = new DefaultMailPartContentResolver();
      IMAPMessage message = mock(IMAPMessage.class);
      when(message.getInputStream()).thenReturn(imapInputStream);
      InputStream content = resolver.resolveInputStream(message);
      assertThat(content, instanceOf(ByteArrayInputStream.class));
      assertThat(new String(toByteArray(content)), equalTo(CONTENT_PAYLOAD));
    }
  }

  @Test
  public void resolveMessagePartContentToOriginalStreamClass() throws Exception {
    try (InputStream imapInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONTENT_FILE)) {
      MailPartContentResolver resolver = new DefaultMailPartContentResolver();
      MimeMessage message = mock(MimeMessage.class);
      when(message.getInputStream()).thenReturn(imapInputStream);
      InputStream content = resolver.resolveInputStream(message);
      assertThat(content, instanceOf(imapInputStream.getClass()));
      assertThat(new String(toByteArray(content)), equalTo(CONTENT_PAYLOAD));
    }
  }


}
