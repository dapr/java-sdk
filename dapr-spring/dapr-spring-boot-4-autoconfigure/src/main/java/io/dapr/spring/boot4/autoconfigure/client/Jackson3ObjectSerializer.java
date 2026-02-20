/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.boot4.autoconfigure.client;

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * {@link DaprObjectSerializer} implementation backed by Jackson 3's {@link JsonMapper}.
 */
public class Jackson3ObjectSerializer implements DaprObjectSerializer {

  private final JsonMapper jsonMapper;

  public Jackson3ObjectSerializer(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public byte[] serialize(Object o) throws IOException {
    if (o == null) {
      return null;
    }

    if (o.getClass() == Void.class) {
      return null;
    }

    if (o instanceof byte[]) {
      return (byte[]) o;
    }

    try {
      return jsonMapper.writeValueAsBytes(o);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IOException(e);
    }
  }

  @Override
  public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
    JavaType javaType = jsonMapper.constructType(type.getType());

    if (javaType.isTypeOrSubTypeOf(Void.class)) {
      return null;
    }

    if (javaType.isPrimitive()) {
      return deserializePrimitives(data, javaType);
    }

    if (data == null) {
      return null;
    }

    if (javaType.hasRawClass(byte[].class)) {
      return (T) data;
    }

    if (data.length == 0) {
      return null;
    }

    try {
      return jsonMapper.readValue(data, javaType);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getContentType() {
    return "application/json";
  }

  @SuppressWarnings("unchecked")
  private <T> T deserializePrimitives(byte[] content, JavaType javaType) throws IOException {
    if (content == null || content.length == 0) {
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

    try {
      return jsonMapper.readValue(content, javaType);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IOException(e);
    }
  }
}
