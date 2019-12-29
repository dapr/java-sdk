/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Serializes and deserializes an object.
 */
public class ObjectSerializer {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Serializes a given object object into byte array.
   *
   * @param object object to be serialized.
   * @return Array of bytes[] with the serialized content.
   * @throws IOException
   */
  public <T> String serialize(T object) throws IOException {
    if (object == null) {
      return null;
    }

    if (object.getClass() == String.class) {
      return object.toString();
    }

    if (isPrimitiveOrEquivalent(object.getClass())) {
      return object.toString();
    }

    // Not string, not primitive, so it is a complex type: we use JSON for that.
    return OBJECT_MAPPER.writeValueAsString(object);
  }

  /**
   * Deserializes the byte array into the original object.
   *
   * @param value  String to be parsed.
   * @param clazz Type of the object being deserialized.
   * @param <T>   Generic type of the object being deserialized.
   * @return Object of type T.
   * @throws IOException
   */
  public <T> T deserialize(String value, Class<T> clazz) throws IOException {
    if (clazz == String.class) {
      return (T) value;
    }

    if (isPrimitiveOrEquivalent(clazz)) {
      return parse(value, clazz);
    }

    // Not string, not primitive, so it is a complex type: we use JSON for that.
    return OBJECT_MAPPER.readValue(value, clazz);
  }

  /**
   * Checks if the class is a primitive or equivalent.
   * @param clazz Class to be checked.
   * @return True if primitive or equivalent.
   */
  protected static boolean isPrimitiveOrEquivalent(Class<?> clazz) {
    if (clazz == null) {
      return false;
    }

    return (clazz.isPrimitive() ||
        (clazz == Boolean.class) ||
        (clazz == Character.class) ||
        (clazz == Byte.class) ||
        (clazz == Short.class) ||
        (clazz == Integer.class) ||
        (clazz == Long.class) ||
        (clazz == Float.class) ||
        (clazz == Double.class) ||
        (clazz == Void.class));
  }

  /**
   * Parses a given String to the corresponding object defined by class.
   * @param value String to be parsed.
   * @param clazz Class of the expected result type.
   * @param <T> Result type.
   * @return Result as corresponding type.
   */
  protected static <T> T parse(String value, Class<T> clazz) {
    if (value == null) {
      if (boolean.class == clazz) return (T) Boolean.FALSE;
      if (byte.class == clazz) return (T) Byte.valueOf((byte) 0);
      if (short.class == clazz) return (T) Short.valueOf((short) 0);
      if (int.class == clazz) return (T) Integer.valueOf(0);
      if (long.class == clazz) return (T) Long.valueOf(0L);
      if (float.class == clazz) return (T) Float.valueOf(0);
      if (double.class == clazz) return (T) Double.valueOf(0);

      return null;
    }

    if ((Boolean.class == clazz) || (boolean.class == clazz)) return (T) Boolean.valueOf(value);
    if ((Byte.class == clazz) || (byte.class == clazz)) return (T) Byte.valueOf(value);
    if ((Short.class == clazz) || (short.class == clazz)) return (T) Short.valueOf(value);
    if ((Integer.class == clazz) || (int.class == clazz)) return (T) Integer.valueOf(value);
    if ((Long.class == clazz) || (long.class == clazz)) return (T) Long.valueOf(value);
    if ((Float.class == clazz) || (float.class == clazz)) return (T) Float.valueOf(value);
    if ((Double.class == clazz) || (double.class == clazz)) return (T) Double.valueOf(value);

    return null;
  }
}
