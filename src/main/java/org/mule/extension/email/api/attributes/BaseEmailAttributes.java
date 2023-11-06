/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.attributes;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import org.mule.extension.email.api.exception.CannotFetchMetadataException;
import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Contains all the basic metadata of a received email, it carries information such as the subject of the email, the number in the
 * mailbox and the recipients between others.
 * <p>
 * This class aims to be returned as attributes for every email message in a {@link PagingProviderEmailDelegate} operation.
 *
 * @since 1.0
 */
public abstract class BaseEmailAttributes implements Serializable {

  /**
   * The number is the relative position of the email in its Folder. Note that the number for a particular email can change during
   * a session if other emails in the Folder are isDeleted and expunged.
   * <p>
   * Valid message ids start at 1. Emails that do not belong to any folder (like newly composed or derived messages) have 0 as
   * their message number.
   */
  @Parameter
  private final int number;

  /**
   * The address(es) of the person(s) which sent the email.
   * <p>
   * This will usually be the sender of the email, but some emails may direct replies to a different address
   */
  @Parameter
  private final List<String> fromAddresses;

  /**
   * The recipient addresses of "To" (primary) type.
   */
  @Parameter
  private final List<String> toAddresses;

  /**
   * The recipient addresses of "Cc" (carbon copy) type
   */
  @Parameter
  private final List<String> ccAddresses;

  /**
   * The recipient addresses of "Bcc" (blind carbon copy) type
   */
  @Parameter
  private final List<String> bccAddresses;

  /**
   * The email addresses to which this email should reply.
   */
  @Parameter
  private final List<String> replyToAddresses;

  /**
   * The headers that this email carry.
   */
  @Parameter
  private final Map<String, String> headers;

  /**
   * The subject of the email.
   */
  @Parameter
  private final String subject;

  /**
   * The time where the email was received.
   * <p>
   * Different {@link Folder} implementations may assign this value or not.
   * <p>
   * If this is a sent email this will be null.
   */
  @Parameter
  @Optional
  private final LocalDateTime receivedDate;

  /**
   * The time where the email was sent.
   * <p>
   * Different {@link Folder} implementations may assign this value or not.
   */
  @Parameter
  @Optional
  private final LocalDateTime sentDate;

  public BaseEmailAttributes(){
    this.number=0;
    this.subject=null;
    this.headers= null;
    this.toAddresses=null;
    this.ccAddresses=null;
    this.bccAddresses=null;
    this.replyToAddresses=null;
    this.sentDate=null;
    this.receivedDate=null;
    this.fromAddresses=null;
  }

  public BaseEmailAttributes(Message msg) {
    try {
      Map<String, String> headers = new HashMap<>();
      list(msg.getAllHeaders()).forEach(h -> headers.put(h.getName(), h.getValue()));

      this.number = msg.getMessageNumber();
      this.subject = msg.getSubject();
      this.headers = Collections.unmodifiableMap(headers);
      this.toAddresses = addressesAsList(msg.getRecipients(TO));
      this.ccAddresses = addressesAsList(msg.getRecipients(CC));
      this.bccAddresses = addressesAsList(msg.getRecipients(BCC));
      this.replyToAddresses = addressesAsList(msg.getReplyTo());
      this.sentDate = asDateTime(msg.getSentDate());
      this.receivedDate = asDateTime(msg.getReceivedDate());
      this.fromAddresses = addressesAsList(msg.getFrom());
    } catch (MessagingException mse) {
      throw new CannotFetchMetadataException(mse.getMessage(), mse);
    }
  }

  /**
   * @return the unique id of the email in a folder.
   */
  public abstract String getId();

  /**
   * Returns the number of the email in the mailbox folder in a moment.
   * <p>
   * Take in mind that this number change with the different operations that can occur in a folder i.e. moving mails, deleting
   * emails, etc.
   *
   * @return the number of the email in the mailbox.
   */
  public int getNumber() {
    return number;
  }

  /**
   * @return the subject of the email.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return all the recipient addresses of "To" (primary) type.
   */
  public List<String> getToAddresses() {
    return toAddresses;
  }

  /**
   * @return all the recipient addresses of "Bcc" (blind carbon copy) type.
   */
  public List<String> getBccAddresses() {
    return bccAddresses;
  }

  /**
   * @return all the recipient addresses of "Cc" (carbon copy) type.
   */
  public List<String> getCcAddresses() {
    return ccAddresses;
  }

  /**
   * Get the identity of the person(s) who wished this message to be sent.
   *
   * @return all the from addresses.
   */
  public List<String> getFromAddresses() {
    return fromAddresses;
  }

  /**
   * Get the addresses to which replies should be directed. This will usually be the sender of the email, but some emails may
   * direct replies to a different address
   *
   * @return all the recipient addresses of replyTo type.
   */
  public List<String> getReplyToAddresses() {
    return replyToAddresses;
  }

  /**
   * Get the date this message was received.
   *
   * @return the date this message was received.
   */
  public LocalDateTime getReceivedDate() {
    return receivedDate;
  }

  /**
   * Get the date this message was sent.
   *
   * @return the date this message was sent.
   */
  public LocalDateTime getSentDate() {
    return sentDate;
  }

  /**
   * @return all the headers of this email message.
   */
  public Map<String, String> getHeaders() {
    return headers != null ? Collections.unmodifiableMap(headers) : Collections.unmodifiableMap(new HashMap<>());
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
  }

  private List<String> addressesAsList(Address[] toAddresses) {
    return toAddresses != null ? stream(toAddresses).map(Object::toString).collect(toImmutableList()) : emptyList();
  }

  private LocalDateTime asDateTime(Date date) {
    if (date != null) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
