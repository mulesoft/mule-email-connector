/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.util.Optional.of;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.BaseMailboxPollingSource;
import org.mule.extension.email.internal.resolver.StoredEmailContentTypeResolver;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

/**
 * Retrieves all the emails from an IMAP mailbox folder, watermark can be enabled for polled items.
 *
 * @since 1.1
 */
@DisplayName("On New Email - IMAP")
@Alias("listener-imap")
@MetadataScope(outputResolver = StoredEmailContentTypeResolver.class)
public class IMAPPollingSource extends BaseMailboxPollingSource {

  /**
   * If watermark should be applied to the polled emails or not. Default to true.
   */
  @Parameter
  @DisplayName("Enable Watermark")
  @Optional(defaultValue = "true")
  private boolean watermarkEnabled;

  /**
   * A matcher to filter emails retrieved by this polling source. For default already read emails will be filtered.
   */
  @Parameter
  @Optional
  private IMAPEmailPredicateBuilder imapMatcher;

  /**
   * If search filters should be resolved on server(true) or client(false) side. Default to false because
   * some email servers might not be fully compliant with rfc-3501's search terms. Activating this feature
   * will diminish traffic by reducing the amount of emails brought to client side for processing.
   * */
  @Parameter
  @DisplayName("Enable Remote Search")
  @Optional(defaultValue = "false")
  private boolean remoteSearchFilterEnabled = false;

  private IMAPRemoteSearchTerm remoteSearchTerm;

  /**
   * {@inheritDoc}
   */
  @Override
  protected java.util.Optional<IMAPEmailPredicateBuilder> getPredicateBuilder() {
    return of(imapMatcher == null ? new DefaultPollingSourceMatcher() : imapMatcher);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isWatermarkEnabled() {
    return watermarkEnabled;
  }

  @Override
  protected void emailDispatchedToFlow() {
    beginUsingFolder();
  }

  @Override
  public void onRejectedItem(Result<StoredEmailContent, BaseEmailAttributes> result,
                             SourceCallbackContext sourceCallbackContext) {
    endUsingFolder();
    super.onRejectedItem(result, sourceCallbackContext);
  }

  @OnBackPressure
  public void onBackPressure() {
    endUsingFolder();
  }

  @OnTerminate
  public void onTerminate() {
    endUsingFolder();
  }

  @Override
  public void doStart() throws ConnectionException {
    super.doStart();
    if (this.getPredicateBuilder().isPresent()) {
      remoteSearchTerm = new IMAPRemoteSearchTerm(this.getPredicateBuilder().get());
    }
  }

  @Override
  protected Message[] getMessages(Folder openFolder) {
    if (!(this.remoteSearchFilterEnabled && getPredicateBuilder().isPresent()
        && this.remoteSearchTerm.getRemoteSearchTerm().isPresent())) {
      //Filters will be applied locally.
      return super.getMessages(openFolder);
    }

    try {
      return openFolder.search(this.remoteSearchTerm.getRemoteSearchTerm().get());
    } catch (MessagingException e) {
      return super.getMessages(openFolder);
    }
  }

}
