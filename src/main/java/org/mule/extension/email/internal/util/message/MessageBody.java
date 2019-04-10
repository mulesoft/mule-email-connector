/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util.message;

import java.util.Collection;

/**
 * Represents the body an email message whether its a multipart message or not.
 *
 * @since 1.2.0
 */
public interface MessageBody {

  /**
   * @return the text of the body part.
   */
  String getText();

  /**
   * @return a collection of {@link MessageAttachment}. If the body has no inline attachments it returns an empty collection.
   */
  Collection<MessageAttachment> getInlineAttachments();

}
