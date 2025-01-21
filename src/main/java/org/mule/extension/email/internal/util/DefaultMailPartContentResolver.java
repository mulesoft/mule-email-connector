/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;

/**
 * Default resolver for a mail part.
 * 
 * @since 1.2.0
 */
public class DefaultMailPartContentResolver implements MailPartContentResolver {

  private static final MailPartContentResolver[] RESOLVERS =
      new MailPartContentResolver[] {
          new IMAPMailPartContentResolver()
      };

  @Override
  public InputStream resolveInputStream(Part part) throws IOException, MessagingException {
    for (MailPartContentResolver resolver : RESOLVERS) {
      if (resolver.resolvesType(part)) {
        return resolver.resolveInputStream(part);
      }
    }
    return part.getInputStream();
  }

  @Override
  public boolean resolvesType(Part part) {
    return true;
  }

}
