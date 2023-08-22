/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.resolver;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.StaticResolver;

/**
 * A {@link StaticResolver} that mimics the structure of a {@link StoredEmailContent}.
 *
 * @since 1.1.2
 */
public class StoredEmailContentTypeResolver extends OutputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    ObjectTypeBuilder message = BaseTypeBuilder.create(JAVA).objectType();
    message.id(StoredEmailContent.class.getName());
    message.addField().required().key("body").value().stringType();
    message.addField().required().key("attachments").value().objectType().openWith().binaryType();
    return message.build();
  }

  @Override
  public String getResolverName() {
    return this.getClass().getSimpleName() + "Resolver";
  }
}
