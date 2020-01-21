/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import java.io.IOException;

/**
 * Serializes and deserializes application's objects.
 */
public interface DaprObjectSerializer {

  /**
   * Serializes the given object as a String to be saved.
   *
   * @param o Object to be serialized.
   * @return Serialized object.
   * @throws IOException If cannot serialize.
   */
  byte[] serialize(Object o) throws IOException;

  /**
   * Deserializes the given String into a object.
   *
   * @param data Data to be deserialized.
   * @param clazz Class of object to be deserialized.
   * @param <T> Type of object to be deserialized.
   * @return Deserialized object.
   * @throws IOException If cannot deserialize object.
   */
  <T> T deserialize(byte[] data, Class<T> clazz) throws IOException;
}
