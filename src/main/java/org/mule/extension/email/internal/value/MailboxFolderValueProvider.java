/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.value;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static java.util.Arrays.stream;

import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * {@link ValueProvider} implementation which provides the list of {@link Folder} on the user namespace.
 * 
 * @since 1.1.0
 */
public class MailboxFolderValueProvider implements ValueProvider {

  @Connection
  private MailboxConnection mailboxConnection;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    try {
      return getValuesFor(stream(mailboxConnection.listFolders()).map(Folder::getFullName));
    } catch (MessagingException e) {
      throw new ValueResolvingException("There has been an error resolving the values for the list of folders", UNKNOWN, e);
    }
  }
}
