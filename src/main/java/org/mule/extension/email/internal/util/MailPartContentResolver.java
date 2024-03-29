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
 * Resolver to obtain the content of a mail part.
 * 
 * @since 1.2.0
 *
 */
public interface MailPartContentResolver {

  InputStream resolveInputStream(Part content) throws IOException, MessagingException;

  boolean resolvesType(Part content);
}
