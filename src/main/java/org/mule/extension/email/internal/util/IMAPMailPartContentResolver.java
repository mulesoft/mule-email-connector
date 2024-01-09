/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static org.mule.runtime.core.api.util.IOUtils.toByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;

import com.sun.mail.imap.IMAPMessage;

/**
 * Resolver for the IMAP mail content part. The content is resolved to an array in memory to avoid problems due to the closing of
 * the folder.
 * 
 * @since 1.2.0
 */
public class IMAPMailPartContentResolver implements MailPartContentResolver {

  @Override
  public InputStream resolveInputStream(Part part) throws IOException, MessagingException {
    if (part.getInputStream() == null) {
      return null;
    }

    return new ByteArrayInputStream(toByteArray(part.getInputStream()));
  }

  @Override
  public boolean resolvesType(Part part) {
    return part instanceof IMAPMessage;
  }

}
