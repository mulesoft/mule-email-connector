/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;


import org.mule.extension.email.internal.errors.EmailError;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.mailbox.imap.IMAPConfiguration;
import org.mule.extension.email.internal.mailbox.pop3.POP3Configuration;
import org.mule.extension.email.internal.sender.SMTPConfiguration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.meta.JavaVersion;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

/**
 * Email connector used to list and send emails and perform operations in different mailboxes, such as delete and mark as read.
 * <p>
 * This connector supports the SMTP, SMTPS, IMAP, IMAPS, POP3 and POP3s protocols.
 *
 * @since 1.0
 */
@Configurations({SMTPConfiguration.class, POP3Configuration.class, IMAPConfiguration.class})
@Extension(name = "Email")
@ErrorTypes(EmailError.class)
@Export(classes = {EmailException.class})
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
public class EmailConnector {

  public static final String TLS_CONFIGURATION = "TLS Configuration";
}
