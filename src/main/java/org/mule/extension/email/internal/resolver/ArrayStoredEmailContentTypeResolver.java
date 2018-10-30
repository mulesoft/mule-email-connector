/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.resolver;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.StaticResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

/**
 * A {@link StaticResolver} that mimics the structure of a {@link StoredEmailContent}.
 *
 * @since 1.1.2
 */
public class ArrayStoredEmailContentTypeResolver extends OutputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    StoredEmailContentTypeResolver delegate = new StoredEmailContentTypeResolver();
    BaseTypeBuilder builder = BaseTypeBuilder.create(JAVA);

    return builder.arrayType().of(new MessageMetadataTypeBuilder()
      .payload(delegate.getStaticMetadata())
      .attributes(builder.voidType().build())
      .with(new ClassInformationAnnotation(Result.class))
      .build()).build();
  }

  @Override
  public String getResolverName() {
    return this.getClass().getSimpleName() + "Resolver";
  }
}
