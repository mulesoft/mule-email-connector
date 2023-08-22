/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.resolver;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.StaticResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import org.mule.extension.email.api.StoredEmailContent;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;

/**
 * A {@link StaticResolver} that mimics the structure of a an array of {@link StoredEmailContent}.
 *
 * @since 1.1.2
 */
public abstract class ArrayStoredEmailContentTypeResolver extends OutputStaticTypeResolver {

  private final ClassTypeLoader LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private final Class attributesType;

  public ArrayStoredEmailContentTypeResolver(Class attributesType) {
    this.attributesType = attributesType;
  }

  @Override
  public MetadataType getStaticMetadata() {
    StoredEmailContentTypeResolver delegate = new StoredEmailContentTypeResolver();
    BaseTypeBuilder builder = BaseTypeBuilder.create(JAVA);

    return builder.arrayType().of(new MessageMetadataTypeBuilder()
        .payload(delegate.getStaticMetadata())
        .attributes(LOADER.load(attributesType))
        .with(new ClassInformationAnnotation(Result.class))
        .build()).build();
  }
}
