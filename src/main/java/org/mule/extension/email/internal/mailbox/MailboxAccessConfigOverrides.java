/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.email.api.attachment.AttachmentNamingStrategy;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class MailboxAccessConfigOverrides {

  /**
   * Indicates how attachment names should be retrieved.
   */
  @ConfigOverride
  @Parameter
  @Placement(tab = ADVANCED_TAB)
  private AttachmentNamingStrategy attachmentNamingStrategy;

  /**
   * @return An { @link AttachmentNaming } that indicates how attachment names should be retrieved.
   */
  public AttachmentNamingStrategy getAttachmentNamingStrategy() {
    return attachmentNamingStrategy;
  }

}
