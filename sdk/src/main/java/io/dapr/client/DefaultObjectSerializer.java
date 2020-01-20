/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import java.io.IOException;

/**
 * Default serializer/deserializer for actor state.
 *
 * WARNING: for production systems, it is recommended for users to provide their own serializer instead.
 */
public class DefaultObjectSerializer implements DaprObjectSerializer {

  /**
   * Shared serializer for all instances of the default state serializer.
   */
  public static final ObjectSerializer SERIALIZER = new ObjectSerializer();

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] serialize(Object o) throws IOException {
    return SERIALIZER.serialize(o);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] data, Class<T> clazz) throws IOException {
    return SERIALIZER.deserialize(data, clazz);
  }
}
