/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TRANSFER_ENCODING_DISPLAY_NAME;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_CONTENT_TRANSFER_ENCODING;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Container group for the email attachments
 *
 * @since 1.0
 */
public final class AttachmentsGroup {

  /**
   * The attachments for an Email, that will be sent along the email body.
   */
  @Optional
  @Parameter
  @Content
  @NullSafe
  private Map<String, TypedValue<InputStream>> attachments;

  /**
   * Encoding used to indicate the type of transformation that has been used in order to represent the body in an
   * acceptable manner for transport. The value is case insensitive.
   * <p>
   * Known encodings:
   * <ul>
   *     <li>BASE64</li>
   *     <li>QUOTED-PRINTABLE</li>
   *     <li>8BIT</li>
   *     <li>7BIT</li>
   *     <li>BINARY</li>
   * </ul>
   *
   * @see <a href="https://www.w3.org/Protocols/rfc1341/5_Content-Transfer-Encoding.html"/>
   */
  @Parameter
  @Summary("Transfer encoding used to send the email attachments. Base64 is recommended for large payloads.")
  @Optional(defaultValue = DEFAULT_CONTENT_TRANSFER_ENCODING)
  @DisplayName(CONTENT_TRANSFER_ENCODING_DISPLAY_NAME)
  private String attachmentsContentTransferEncoding;

  /**
   * @return a {@link List} of attachments configured in the built outgoing email.
   */
  public Map<String, TypedValue<InputStream>> getAttachments() {
    return attachments;
  }

  public void setAttachments(Map<String, TypedValue<InputStream>> attachments) {
    this.attachments = attachments;
  }

  public String getContentTransferEncoding() {
    return attachmentsContentTransferEncoding;
  }

  public void setContentTransferEncoding(String contentTransferEncoding) {
    this.attachmentsContentTransferEncoding = contentTransferEncoding;
  }
}
