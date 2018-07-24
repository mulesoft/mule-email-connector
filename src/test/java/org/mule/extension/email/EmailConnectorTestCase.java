/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.setUpServer;
import static org.mule.functional.api.exception.ExpectedError.none;

import org.mule.extension.email.api.EmailError;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;

import java.io.IOException;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"com.sun.mail:javax.mail"},
    testExclusions = {"org.mule.module:mule-java-module"},
    exportPluginClasses = EmailError.class)
public abstract class EmailConnectorTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String NAMESPACE = "EMAIL";

  @Rule
  public DynamicPort PORT = new DynamicPort("port");

  @Rule
  public ExpectedError expectedError = none();

  protected GreenMail server;
  protected GreenMailUser user;

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    ServerSetup serverSetup = setUpServer(PORT.getNumber(), getProtocol());
    server = new GreenMail(serverSetup);
    server.start();
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "password");
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    assertThat(server, is(not(nullValue())));
    server.stop();
  }

  protected void assertBodyContent(String content) {
    assertThat(content, is(EMAIL_CONTENT));
  }

  protected void assertSubject(String content) {
    assertThat(content, is(EMAIL_SUBJECT));
  }

  protected void assertMessage(Message message, MediaType mediaType) throws MessagingException {
    assertThat(MediaType.parse(message.getHeader("Content-Type")[0]).withoutParameters(), is(mediaType));
  }

  protected void assertMessage(Message message, String expectedMessageBody, MediaType expectedMediaType)
      throws IOException, MessagingException {
    assertThat(IOUtils.toString(message.getInputStream()).trim(), is(expectedMessageBody));
    assertThat(message.getHeader("Content-Type")[0], is(expectedMediaType.toRfcString()));
  }

  protected void assertMessage(BodyPart bodyPart, String expectedMessageBody, MediaType expectedMediaType)
      throws IOException, MessagingException {
    assertThat(IOUtils.toString(bodyPart.getInputStream()).trim(), is(expectedMessageBody));
    assertThat(bodyPart.getContentType(), is(expectedMediaType.toRfcString()));
  }

  public abstract String getProtocol();
}
