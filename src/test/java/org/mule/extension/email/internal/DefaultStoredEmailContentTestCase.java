/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.InputStream;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultStoredEmailContentTestCase {

  @Test
  public void serializationWithAttachments() {
    Map<String, TypedValue<InputStream>> attachments = ImmutableMap.<String, TypedValue<InputStream>>builder()
        .put("attachment1", new TypedValue<>(null, null))
        .put("attachment2", new TypedValue<>(null, null)).build();
    DefaultStoredEmailContent email = new DefaultStoredEmailContent(new TypedValue<>("this is the body", null), attachments);
    assertThat(email.toString(),
               is("{\n \"body\": \"this is the body\", \n \"attachments\": [\"attachment1\", \"attachment2\"]\n}"));
  }

  @Test
  public void serialization() {
    DefaultStoredEmailContent email = new DefaultStoredEmailContent(new TypedValue<>("this is the body", null), null);
    assertThat(email.toString(), is("{\n \"body\": \"this is the body\", \n \"attachments\": []\n}"));
  }
}
