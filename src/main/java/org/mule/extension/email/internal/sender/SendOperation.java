/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;


import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.api.metadata.DataType.fromObject;

import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.internal.commands.SendCommand;
import org.mule.extension.email.internal.errors.SendErrorTypeProvider;
import org.mule.extension.email.internal.util.AttachmentsGroup;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import javax.inject.Inject;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic set of operations which perform send email operations over the SMTP or SMTPS protocol.
 *
 * @since 1.0
 */
public class SendOperation {

  private final SendCommand sendCommand = new SendCommand();

  @Inject
  private TransformationService transformationService;

  /**
   * Sends an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the message.
   * <p>
   * The content of the message aims to be some type of text (text/plan, text/html) and its composed by the body and it's content
   * type. If no content is specified then the incoming payload it's going to be converted into plain text if possible.
   *
   * @param connection    Connection to use to send the message
   * @param configuration Configuration of the connector
   * @param settings  The builder of the email that is going to be send.
   */
  @Summary("Sends an email message")
  @Throws(SendErrorTypeProvider.class)
  public void send(@Connection SenderConnection connection,
                   @Config SMTPConfiguration configuration,
                   @Placement(order = 1) @ParameterGroup(name = "Settings") EmailSettings settings,
                   @Placement(order = 2) @ParameterGroup(name = "Body", showInDsl = true) EmailBody body,
                   @Placement(order = 3) @ParameterGroup(name = "Attachments") AttachmentsGroup attachments)
      throws MessageTransformerException, TransformerException, EmailConnectionException {
    attachments.setAttachments(transformAttachments(attachments));
    sendCommand.send(connection, configuration, settings, body, attachments);
  }

  /**
   * A utility method that ensures that all attachments are of type {@link InputStream},
   * otherwise they will be transformed.
   *
   * @param attachments to ensure and transform.
   * @return a new {@link Map} of attachments represented in {@link InputStream}
   */
  private Map<String, TypedValue<InputStream>> transformAttachments(AttachmentsGroup attachments)
      throws MessageTransformerException, TransformerException {
    Map<String, TypedValue<InputStream>> newAttachments = new LinkedHashMap<>();
    for (Map.Entry<String, TypedValue<InputStream>> attachment : attachments.getAttachments().entrySet()) {
      newAttachments.put(attachment.getKey(), getTransformTypedValue(attachment.getValue()));
    }
    return newAttachments;
  }

  private TypedValue<InputStream> getTransformTypedValue(TypedValue typedValue)
      throws MessageTransformerException, TransformerException {

    Object value = typedValue.getValue();
    if (value instanceof InputStream) {
      return typedValue;
    }
    InputStream result = (InputStream) transformationService.transform(value, fromObject(value), INPUT_STREAM);
    return new TypedValue<>(result, DataType.builder().type(result.getClass()).mediaType(typedValue.getDataType().getMediaType())
        .build());
  }
}
