/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Serializes and deserializes an object.
 */
class ActorStateSerializer {

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
    <T> String serialize(T state) throws IOException {
        if (state == null) {
            return null;
        }

        if (state.getClass() == String.class) {
            return state.toString();
        }

        if (isPrimitive(state.getClass())) {
            return state.toString();
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.writeValueAsString(state);
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
    <T> T deserialize(String value, Class<T> clazz) throws IOException {
        if (clazz == String.class) {
            return (T) value;
        }

        if (isPrimitive(clazz)) {
            return parse(value, clazz);
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.readValue(value, clazz);
    }

    private static boolean isPrimitive(Class<?> clazz) {
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

    private static <T> T parse(String value, Class<T> clazz) {
        if (value == null) {
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
