/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.POP3EmailAttributes;
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import com.sun.mail.pop3.POP3Folder;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * Configuration for operations that are performed through the POP3 (Post Office Protocol 3) protocol.
 *
 * @since 1.0
 */
@Operations(POP3Operations.class)
@ConnectionProviders({POP3Provider.class, POP3SProvider.class})
@Configuration(name = "pop3")
@DisplayName("POP3")
@Sources(POP3PollingSource.class)
public class POP3Configuration implements MailboxAccessConfiguration {

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
   * {@inheritDoc}
   */
  @Override
  public AttachmentNamingStrategy getAttachmentNamingStrategy() {
    return attachmentNamingStrategy;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The pop3 protocol always read the content when retrieves an email.
   */
  @Override
  public boolean isEagerlyFetchContent() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public <T extends BaseEmailAttributes> T parseAttributesFromMessage(Message message, Folder folder) {
    return (T) new POP3EmailAttributes(message, (POP3Folder) folder);
  }
}
