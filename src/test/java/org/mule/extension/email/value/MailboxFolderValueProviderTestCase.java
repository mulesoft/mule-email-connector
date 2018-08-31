/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.value;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.inject.Inject;

import com.icegreen.greenmail.imap.ImapHostManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MailboxFolderValueProviderTestCase extends EmailConnectorTestCase {

  private static final String SPECIAL_CHARACTER_PASSWORD = "*uawH*IDXlh2p%21xSPOx%23%25zLpL";
  private static final String SALESFORCE_FOLDER = "salesforce";
  private static final String MULESOFT_FOLDER = SALESFORCE_FOLDER + "/mulesoft";

  @Rule
  public SystemProperty specialCharacterPassword = new SystemProperty("specialCharacterPassword", SPECIAL_CHARACTER_PASSWORD);

  @Inject
  private ValueProviderService service;


  @Override
  protected String[] getConfigFiles() {
    return new String[] {"retriever/imap.xml", "retriever/imap-flows.xml"};
  }

  @Override
  public String getProtocol() {
    return "imap";
  }

  @Before
  public void setup() throws Exception {
    ImapHostManager imapHostManager = server.getManagers().getImapHostManager();
    imapHostManager.createMailbox(user, SALESFORCE_FOLDER);
    imapHostManager.createMailbox(user, MULESOFT_FOLDER);
  }

  @Test
  public void mailboxFolderAsValueProvider() {
    ValueResult values =
        service.getValues(Location.builder().globalName("retrieveAndDontRead").addProcessorsPart().addIndexPart(0).build(),
                          "mailboxFolder");

    assertThat(values.isSuccess(), is(true));
    assertThat(values.getValues(),
               hasItems(valueWithId("INBOX"), valueWithId(SALESFORCE_FOLDER), valueWithId(MULESOFT_FOLDER)));
  }

}
