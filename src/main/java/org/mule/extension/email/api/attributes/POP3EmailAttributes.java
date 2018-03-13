/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.attributes;

import static java.lang.Long.parseLong;
import org.mule.extension.email.api.exception.CannotFetchMetadataException;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.sun.mail.pop3.POP3Folder;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Contains all the metadata of a received email from a POP3 mailbox, it carries information such as the subject of the email,
 * the unique id in the mailbox and the recipients between others
 *
 * @since 1.0
 */
public class POP3EmailAttributes extends BaseEmailAttributes {

  private static final long serialVersionUID = -982204133196288278L;

  /**
   * The unique identifier of the email in a mailbox folder.
   */
  @Parameter
  private final String id;

  /**
   * Creates a new instance from a {@link Message}
   *
   * @param msg an email message to take the attributes from.
   */
  public POP3EmailAttributes(Message msg, POP3Folder folder) {
    super(msg);
    try {
      this.id = folder.getUID(msg);
    } catch (MessagingException e) {
      throw new CannotFetchMetadataException("Could not initialize POP3 attributes", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return id;
  }
}
