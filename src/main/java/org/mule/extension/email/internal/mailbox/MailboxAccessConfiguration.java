/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * Generic contract for configurations that contains operations for retrieving and managing emails in a mailbox.
 *
 * @since 1.0
 */
public interface MailboxAccessConfiguration {

  /**
   * @return An { @link AttachmentNaming } that indicates how attachment names should be retrieved.
   */
  AttachmentNamingStrategy getAttachmentNamingStrategy();

  /**
   * @return a boolean value that indicates whether the retrieved emails should be opened and read or not.
   */
  boolean isEagerlyFetchContent();

  /**
   * Resolves the {@link BaseEmailAttributes} from a given message for this configuration.
   *
   * @param message the {@link Message} that we want to parse.
   * @param folder  the folder used to find the id of the message in it.
   * @return an {@link BaseEmailAttributes} instance from the parsed {@code message}.
   */
  <T extends BaseEmailAttributes> T parseAttributesFromMessage(Message message, Folder folder);

}
