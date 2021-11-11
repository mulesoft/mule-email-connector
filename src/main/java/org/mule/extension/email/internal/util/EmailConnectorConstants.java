/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;

/**
 * this class contains common methods for email handling.
 *
 * @since 1.0
 */
public final class EmailConnectorConstants {

  /**
   * Default folder name for all the mailboxes.
   */
  public static final String INBOX_FOLDER = "INBOX";

  /**
   * defines all the multipart content types
   */
  public static final String MULTIPART = "multipart/*";

  /**
   * defines all the text content types
   */
  public static final String TEXT = "text/*";

  /**
   * Default port value for SMTP servers.
   */
  public static final String SMTP_PORT = "25";

  /**
   * Default port value for SMTPS servers.
   */
  public static final String SMTPS_PORT = "465";

  /**
   * Default port value for POP3 servers.
   */
  public static final String POP3_PORT = "110";

  /**
   * Default port value for POP3S servers.
   */
  public static final String POP3S_PORT = "995";

  /**
   * Default port value for IMAP servers.
   */
  public static final String IMAP_PORT = "143";

  /**
   * Default port value for IMAPS servers.
   */
  public static final String IMAPS_PORT = "993";

  /**
   * Email retrieval operations don't have by default a limit for the amount of emails that can be retrieved.
   */
  public static final String UNLIMITED = "-1";

  /**
   * The content type header name.
   */
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  /**
   * Content transfer encoding header name.
   */
  public static final String CONTENT_TRANSFER_ENCODING_HEADER = "Content-Transfer-Encoding";

  /**
   * Default value for the Content-Transfer-Encoding header.
   */
  public static final String DEFAULT_CONTENT_TRANSFER_ENCODING = "Base64";

  /**
   * Default page size to be fetched by the {@link PagingProviderEmailDelegate}
   */
  public static final String DEFAULT_PAGE_SIZE = "10";

  public static final String PAGE_SIZE_ERROR_MESSAGE = "Page size attribute must be greater than zero but '%d' was received";

  /**
   * Display name for parameters that describe the Content-Transfer-Encoding of a Email part
   */
  public static final String CONTENT_TRANSFER_ENCODING_DISPLAY_NAME = "Content Transfer Encoding";

  /**
   * Connector overrides parameter group name
   */
  public static final String CONFIG_OVERRIDES_PARAM_GROUP = "Configuration Overrides";

  /**
   * Hide constructor
   */
  private EmailConnectorConstants() {}

  /**
   * System property name that allows parsing text file attachment as body message
   */
  public static final String PARSING_TEXT_ATTACHMENT_AS_BODY = "parsing.text.attachment.as.body";
}
