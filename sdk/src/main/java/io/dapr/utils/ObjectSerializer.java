/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Serializes and deserializes an object.
 */
public class ObjectSerializer {

    /**
     * Shared Json Factory as per Jackson's documentation, used only for this class.
     */
    protected static final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * Shared Json serializer/deserializer as per Jackson's documentation.
     */
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Serializes a given state object into byte array.
     *
     * @param state State object to be serialized.
     * @param <T>   Type of the state object.
     * @return Array of bytes[] with the serialized content.
     * @throws IOException In case state cannot be serialized.
     */
    public <T> String serialize(T state) throws IOException {
        if (state == null) {
            return null;
        }

        if (state.getClass() == String.class) {
            return state.toString();
        }

        if (isPrimitiveOrEquivalent(state.getClass())) {
            return state.toString();
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.writeValueAsString(state);
    }

    /**
     * Deserializes the byte array into the original object.
     *
     * @param value Content to be parsed.
     * @param clazz Type of the object being deserialized.
     * @param <T>   Generic type of the object being deserialized.
     * @return Object of type T.
     * @throws IOException In case value cannot be deserialized.
     */
    public <T> T deserialize(Object value, Class<T> clazz) throws IOException {
        if (clazz == String.class) {
            return (T) value;
        }

        if (isPrimitiveOrEquivalent(clazz)) {
            return parse(value, clazz);
        }

        if (value == null) {
            return (T) null;
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        if (value instanceof byte[]) {
            return OBJECT_MAPPER.readValue((byte[]) value, clazz);
        }

        return OBJECT_MAPPER.readValue(value.toString(), clazz);
    }

    /**
     * Checks if the class is a primitive or equivalent.
     *
     * @param clazz Class to be checked.
     * @return True if primitive or equivalent.
     */
    private static boolean isPrimitiveOrEquivalent(Class<?> clazz) {
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
     *
     * @param value Value to be parsed.
     * @param clazz Class of the expected result type.
     * @param <T>   Result type.
     * @return Result as corresponding type.
     */
    private static <T> T parse(Object value, Class<T> clazz) {
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

        if (!(value instanceof String)) {
            if (isBooleanOrPrimitive(clazz) && isBooleanOrPrimitive(value.getClass())) return (T) value;
            if (isByteOrPrimitive(clazz) && isByteOrPrimitive(value.getClass())) return (T) value;
            if (isShortOrPrimitive(clazz) && isShortOrPrimitive(value.getClass())) return (T) value;
            if (isIntegerOrPrimitive(clazz) && isIntegerOrPrimitive(value.getClass())) return (T) value;
            if (isLongOrPrimitive(clazz) && isLongOrPrimitive(value.getClass())) return (T) value;
            if (isFloatOrPrimitive(clazz) && isFloatOrPrimitive(value.getClass())) return (T) value;
            if (isDoubleOrPrimitive(clazz) && isDoubleOrPrimitive(value.getClass())) return (T) value;
        }

        if (isBooleanOrPrimitive(clazz)) return (T) Boolean.valueOf(value.toString());
        if (isByteOrPrimitive(clazz)) return (T) Byte.valueOf(value.toString());
        if (isShortOrPrimitive(clazz)) return (T) Short.valueOf(value.toString());
        if (isIntegerOrPrimitive(clazz)) return (T) Integer.valueOf(value.toString());
        if (isLongOrPrimitive(clazz)) return (T) Long.valueOf(value.toString());
        if (isFloatOrPrimitive(clazz)) return (T) Float.valueOf(value.toString());
        if (isDoubleOrPrimitive(clazz)) return (T) Double.valueOf(value.toString());

        return null;
    }

    private static boolean isBooleanOrPrimitive(Class<?> clazz) {
        return (Boolean.class == clazz) || (boolean.class == clazz);
    }

    private static boolean isByteOrPrimitive(Class<?> clazz) {
        return (Byte.class == clazz) || (byte.class == clazz);
    }

    private static boolean isShortOrPrimitive(Class<?> clazz) {
        return (Short.class == clazz) || (short.class == clazz);
    }

    private static boolean isIntegerOrPrimitive(Class<?> clazz) {
        return (Integer.class == clazz) || (int.class == clazz);
    }

    private static boolean isLongOrPrimitive(Class<?> clazz) {
        return (Long.class == clazz) || (long.class == clazz);
    }

    private static boolean isFloatOrPrimitive(Class<?> clazz) {
        return (Float.class == clazz) || (float.class == clazz);
    }

    private static boolean isDoubleOrPrimitive(Class<?> clazz) {
        return (Double.class == clazz) || (double.class == clazz);
    }
}
