/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;


import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.CONTENT_TRANSFER_ENCODING_DISPLAY_NAME;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_CONTENT_TRANSFER_ENCODING;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents and enables the construction of the body of an email with a body of type "text/*" and a specific character
 * encoding.
 *
 * @since 1.0
 */
@XmlHints(allowTopLevelDefinition = true)
public class EmailBody {

  /**
   * Text body of the message. Aims to be text in any format
   */
  @Parameter
  @Content(primary = true)
  @Placement(order = 1)
  @Summary("Text body of the message")
  private TypedValue<InputStream> content;

  /**
   * ContentType of the body text. Example: "text/plain".
   */
  @Parameter
  @Optional
  @Example("text/plain")
  @DisplayName("ContentType")
  @Placement(order = 2)
  @Summary("The content type of the body's content text")
  private String contentType;

  /**
   * The character encoding of the body. If it is configured, it overrides the one inferred from the body.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  @Example("UTF-8")
  @Summary("The character encoding of the body. If it is configured, it overrides the one inferred from the body")
  private String encoding;

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
  @Optional(defaultValue = DEFAULT_CONTENT_TRANSFER_ENCODING)
  @DisplayName(CONTENT_TRANSFER_ENCODING_DISPLAY_NAME)
  @Placement(order = 4)
  @Summary("Transfer encoding used to send the body content. Base64 is recommended for large payloads.")
  private String contentTransferEncoding;

  public EmailBody() {}

  public EmailBody(TypedValue<InputStream> content, String contentType, String encoding) {
    this.content = content;
    this.contentType = contentType;
    this.encoding = encoding;
  }

  public TypedValue<InputStream> getContent() {
    return content;
  }

  /**
   * @return the contentType of the body. one of "text/html" or "text/plain"
   */
  public java.util.Optional<MediaType> getContentType() {
    return isNull(contentType) ? empty() : of(DataType.builder().mediaType(contentType).build().getMediaType());
  }

  /**
   * @return the encoding of the body.
   */
  public String getOverrideEncoding() {
    return encoding;
  }

  /**
   * @return the body of the message content. The body aims to be text.
   */
  public String getContentAsString(Charset charset) throws IOException {
    return isNull(content) || isNull(content.getValue()) ? "" : IOUtils.toString(content.getValue(), charset);
  }

  public String getContentTransferEncoding() {
    return contentTransferEncoding;
  }
}
