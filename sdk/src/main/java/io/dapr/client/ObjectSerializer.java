/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.MessageLite;
import io.dapr.client.domain.CloudEvent;
import io.dapr.utils.TypeRef;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Serializes and deserializes an internal object.
 */
public class ObjectSerializer {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  /**
   * Default constructor to avoid class from being instantiated outside package but still inherited.
   */
  protected ObjectSerializer() {
  }

  protected ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
  }

  /**
   * Serializes a given state object into byte array.
   *
   * @param state State object to be serialized.
   * @return Array of bytes[] with the serialized content.
   * @throws IOException In case state cannot be serialized.
   */
  public byte[] serialize(Object state) throws IOException {
    if (state == null) {
      return null;
    }

    if (state.getClass() == Void.class) {
      return null;
    }

    // Have this check here to be consistent with deserialization (see deserialize() method below).
    if (state instanceof byte[]) {
      return (byte[]) state;
    }

    // Proto buffer class is serialized directly.
    if (state instanceof MessageLite) {
      return ((MessageLite) state).toByteArray();
    }

    // Not string, not primitive, so it is a complex type: we use JSON for that.
    return getObjectMapper().writeValueAsBytes(state);
  }

  /**
   * Deserializes the byte array into the original object.
   *
   * @param content Content to be parsed.
   * @param type    Type of the object being deserialized.
   * @param <T>     Generic type of the object being deserialized.
   * @return Object of type T.
   * @throws IOException In case content cannot be deserialized.
   */
  public <T> T deserialize(byte[] content, TypeRef<T> type) throws IOException {
    return deserialize(content, getObjectMapper().constructType(type.getType()));
  }

  /**
   * Deserializes the byte array into the original object.
   *
   * @param content Content to be parsed.
   * @param clazz   Type of the object being deserialized.
   * @param <T>     Generic type of the object being deserialized.
   * @return Object of type T.
   * @throws IOException In case content cannot be deserialized.
   */
  public <T> T deserialize(byte[] content, Class<T> clazz) throws IOException {
    return deserialize(content, getObjectMapper().constructType(clazz));
  }

  private <T> T deserialize(byte[] content, JavaType javaType) throws IOException {
    if ((javaType == null) || javaType.isTypeOrSubTypeOf(Void.class)) {
      return null;
    }

    if (javaType.isPrimitive()) {
      return deserializePrimitives(content, javaType);
    }

    if (content == null) {
      return null;
    }

    // Deserialization of GRPC response fails without this check since it does not come as base64 encoded byte[].
    if (javaType.hasRawClass(byte[].class)) {
      return (T) content;
    }

    if (content.length == 0) {
      return null;
    }

    if (javaType.hasRawClass(CloudEvent.class)) {
      return (T) CloudEvent.deserialize(content);
    }

    if (javaType.isTypeOrSubTypeOf(MessageLite.class)) {
      try {
        Method method = javaType.getRawClass().getDeclaredMethod("parseFrom", byte[].class);
        if (method != null) {
          return (T) method.invoke(null, content);
        }
      } catch (NoSuchMethodException e) {
        // It was a best effort. Skip this try.
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    return getObjectMapper().readValue(content, javaType);
  }

  /**
   * Parses the JSON content into a node for fine-grained processing.
   *
   * @param content JSON content.
   * @return JsonNode.
   * @throws IOException In case content cannot be parsed.
   */
  public JsonNode parseNode(byte[] content) throws IOException {
    return  getObjectMapper().readTree(content);
  }

  /**
   * Parses a given String to the corresponding object defined by class.
   *
   * @param content  Value to be parsed.
   * @param javaType Type of the expected result type.
   * @param <T>      Result type.
   * @return Result as corresponding type.
   * @throws IOException if cannot deserialize primitive time.
   */
  private static <T> T deserializePrimitives(byte[] content, JavaType javaType) throws IOException {
    if ((content == null) || (content.length == 0)) {
      if (javaType.hasRawClass(boolean.class)) {
        return (T) Boolean.FALSE;
      }

      if (javaType.hasRawClass(byte.class)) {
        return (T) Byte.valueOf((byte) 0);
      }

      if (javaType.hasRawClass(short.class)) {
        return (T) Short.valueOf((short) 0);
      }

      if (javaType.hasRawClass(int.class)) {
        return (T) Integer.valueOf(0);
      }

      if (javaType.hasRawClass(long.class)) {
        return (T) Long.valueOf(0L);
      }

      if (javaType.hasRawClass(float.class)) {
        return (T) Float.valueOf(0);
      }

      if (javaType.hasRawClass(double.class)) {
        return (T) Double.valueOf(0);
      }

      if (javaType.hasRawClass(char.class)) {
        return (T) Character.valueOf(Character.MIN_VALUE);
      }

      return null;
    }

    return OBJECT_MAPPER.readValue(content, javaType);
  }
}
