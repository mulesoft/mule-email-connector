/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import com.sun.mail.imap.IMAPFolder;

import jakarta.mail.Folder;
import jakarta.mail.Message;

/**
 * Configuration for operations that are performed through the IMAP (Internet Message Access Protocol) protocol.
 *
 * @since 1.0
 */
@Operations(IMAPOperations.class)
@ConnectionProviders({IMAPProvider.class, IMAPSProvider.class})
@Configuration(name = "imap")
@DisplayName("IMAP")
@Sources(IMAPPollingSource.class)
public class IMAPConfiguration implements MailboxAccessConfiguration {

  /**
   * Indicates how attachment names should be retrieved.
   */
  @DisplayName("Attachment Naming Strategy")
  @Summary("Defines which strategy must be used when searching for the attachment name")
  @Parameter
  @Placement(tab = ADVANCED_TAB)
  @Optional(defaultValue = "NAME")
  private AttachmentNamingStrategy attachmentNamingStrategy;

  /**
   * Indicates whether the retrieved emails should be opened and read. The default value is "true".
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean eagerlyFetchContent;

  /**
   * {@inheritDoc}
   */
  @Override
  public AttachmentNamingStrategy getAttachmentNamingStrategy() {
    return attachmentNamingStrategy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEagerlyFetchContent() {
    return eagerlyFetchContent;
  }

  @Override
  public <T extends BaseEmailAttributes> T parseAttributesFromMessage(Message message, Folder folder) {
    return (T) new IMAPEmailAttributes(message, (IMAPFolder) folder);
  }
}
