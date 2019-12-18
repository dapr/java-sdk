/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Serializes and deserializes an object.
 */
class ActorStateProviderSerializer {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Serializes a given state object into byte array.
   *
   * @param state State object to be serialized.
   * @return Array of bytes[] with the serialized content.
   * @throws IOException
   */
  String serialize(Object state) throws IOException {
    return OBJECT_MAPPER.writeValueAsString(state);
  }

  /**
   * Deserializes the byte array into the original object.
   *
   * @param json String to be parsed.
   * @param clazz Type of the object being deserialized.
   * @param <T> Generic type of the object being deserialized.
   * @return Object of type T.
   * @throws IOException
   */
  <T> T deserialize(String json, Class<T> clazz) throws IOException {
    return OBJECT_MAPPER.readValue(json, clazz);
  }

}
