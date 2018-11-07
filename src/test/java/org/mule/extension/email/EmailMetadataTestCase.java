/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.junit.Test;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.attributes.POP3EmailAttributes;
import org.mule.extension.email.internal.resolver.IMAPArrayStoredEmailContentTypeResolver;
import org.mule.extension.email.internal.resolver.POP3ArrayStoredEmailContentTypeResolver;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.utils.MetadataTypeUtils;
import org.mule.metadata.message.api.MessageMetadataType;

public class EmailMetadataTestCase {

  @Test
  public void pop3List() {
    MetadataType pop3ListMetadata = new POP3ArrayStoredEmailContentTypeResolver().getStaticMetadata();
    assertListType(pop3ListMetadata, POP3EmailAttributes.class.getName(), 11);
  }

  @Test
  public void imapList() {
    MetadataType imapListMetadata = new IMAPArrayStoredEmailContentTypeResolver().getStaticMetadata();
    assertListType(imapListMetadata, IMAPEmailAttributes.class.getName(), 12);
  }

  private void assertListType(MetadataType metadataType, String attributesTypeId, int attributesSize) {
    assertThat(metadataType, instanceOf(ArrayType.class));
    ArrayType arrayType = ((ArrayType) metadataType);
    assertThat(arrayType.getType(), instanceOf(MessageMetadataType.class));
    MessageMetadataType messageType = (MessageMetadataType) arrayType.getType();
    ObjectType payloadType = ((ObjectType) messageType.getPayloadType().get());
    assertThat(payloadType.getFields(), hasSize(2));
    ObjectType attributesType = ((ObjectType) messageType.getAttributesType().get());
    assertThat(attributesType.getFields(), hasSize(attributesSize));
    assertThat(MetadataTypeUtils.getTypeId(attributesType).get(), is(attributesTypeId));
  }

}
