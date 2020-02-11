/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.serializer;

import io.dapr.client.ObjectSerializer;

import java.io.IOException;

/**
 * Default serializer/deserializer for request/response objects and for state objects too.
 */
public class DefaultObjectSerializer extends ObjectSerializer implements DaprObjectSerializer {

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] serialize(Object o) throws IOException {
    return super.serialize(o);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
    return super.deserialize(data, clazz);
  }
}
