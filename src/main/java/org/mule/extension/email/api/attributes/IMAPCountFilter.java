/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.attributes;

/**
 * IMAP messages counting filter option.
 *
 *  @since 1.7
 */
public enum IMAPCountFilter {
  ALL, DELETED, NEW, UNREAD
}
