/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.ObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.IOException;

/**
 * Default serializer/deserializer for request/response objects and for state objects too.
 */
public class DefaultObjectSerializer extends ObjectSerializer implements DaprObjectSerializer {
  public DefaultObjectSerializer() {
    super();
  }

  public DefaultObjectSerializer(ObjectMapper objectMapper) {
    super(objectMapper);
  }

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
  public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
    return super.deserialize(data, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType() {
    return "application/json";
  }
}
