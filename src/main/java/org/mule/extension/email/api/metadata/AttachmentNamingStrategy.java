/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.metadata;

/**
 * Indicates how attachment names should be retrieved.
 *
 * @since 1.2
 */
public enum AttachmentNamingStrategy {

  /**
   * Look at the actual attachment name only.
   */
  DEFAULT,

  /**
   * Look at the attachment name, if is is not found, also look at the 'name' header.
   */
  NAME_HEADERS,

  /**
   * Look at the attachment name, then 'name' header, then (if the attachment is an email) at it's subject.
   */
  NAME_HEADERS_SUBJECT
}
