/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.runtime.Dapr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Constant result for empty maps.
     */
    private static final Map<String, Object> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<>());

    /**
     * Serializes a given state object into byte array.
     *
     * @param state State object to be serialized.
     * @param <T>   Type of the state object.
     * @return Array of bytes[] with the serialized content.
     * @throws IOException In case state cannot be serialized.
     */
    public <T> byte[] serialize(T state) throws IOException {
        if (state == null) {
            return null;
        }

        if (state instanceof byte[]) {
            return (byte[])state;
        }

        if (state.getClass() == String.class) {
            return ((String) state).getBytes(StandardCharsets.UTF_8);
        }

        if (isPrimitiveOrEquivalent(state.getClass())) {
            return state.toString().getBytes(StandardCharsets.UTF_8);
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.writeValueAsBytes(state);
    }

    /**
     * Serializes a given state object into String.
     *
     * @param state State object to be serialized.
     * @param <T>   Type of the state object.
     * @return Array of bytes[] with the serialized content.
     * @throws IOException In case state cannot be serialized.
     */
    public <T> String serializeString(T state) throws IOException {
        if (state == null) {
            return null;
        }

        if (state.getClass() == String.class) {
            return (String) state;
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
        if (isPrimitiveOrEquivalent(clazz)) {
            return parse(value, clazz);
        }

        if (value == null) {
            return (T) null;
        }

        if (clazz == Dapr.Message.class) {
            return (T) this.deserializeTopicMessage(value);
        }

        if (clazz == String.class) {
            return (value instanceof byte[])
                ? (T) new String((byte[])value, StandardCharsets.UTF_8) : (T) value.toString();
        }

        if (clazz == byte[].class) {
            if (value instanceof String) {
                return (T) value.toString().getBytes(StandardCharsets.UTF_8);
            }

            return (value instanceof byte[])
                ? (T) value : null;
        }

        // Not string, not primitive, not byte[], so it is a complex type: we use JSON for that.
        if (value instanceof byte[]) {
            return OBJECT_MAPPER.readValue((byte[]) value, clazz);
        }

        return OBJECT_MAPPER.readValue(value.toString(), clazz);
    }

    /**
     * Deserialized a message topic from Dapr.
     * @param payload Payload sent from Dapr.
     * @return Message (can be null if input is null)
     * @throws IOException If cannot parse.
     */
    private Dapr.Message deserializeTopicMessage(Object payload) throws IOException {
        if (payload == null) {
            return null;
        }

        JsonNode node = null;
        if (payload instanceof byte[]) {
            node = OBJECT_MAPPER.readTree((byte[])payload);
        } else {
            node = OBJECT_MAPPER.readTree(payload.toString());
        }

        if (node== null) {
            return null;
        }

        String id = null;
        if (node.has("id") && !node.get("id").isNull()) {
            id = node.get("id").asText();
        }

        String dataType = null;
        if (node.has("datacontenttype") && !node.get("datacontenttype").isNull()) {
            dataType = node.get("datacontenttype").asText();
        }

        byte[] data = null;
        if (node.has("data") && !node.get("data").isNull()) {
            try {
                data = node.get("data").binaryValue();
            } catch (IOException e) {
                data = node.get("data").textValue().getBytes(StandardCharsets.UTF_8);
            }
        }

        return new Dapr.Message(id, dataType, data, null);
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

        String valueString = (value instanceof byte[]) ?
            new String((byte[])value, StandardCharsets.UTF_8) : value.toString();

        if (isBooleanOrPrimitive(clazz)) return (T) Boolean.valueOf(valueString);
        if (isByteOrPrimitive(clazz)) return (T) Byte.valueOf(valueString);
        if (isShortOrPrimitive(clazz)) return (T) Short.valueOf(valueString);
        if (isIntegerOrPrimitive(clazz)) return (T) Integer.valueOf(valueString);
        if (isLongOrPrimitive(clazz)) return (T) Long.valueOf(valueString);
        if (isFloatOrPrimitive(clazz)) return (T) Float.valueOf(valueString);
        if (isDoubleOrPrimitive(clazz)) return (T) Double.valueOf(valueString);

        return null;
    }

    /**
     * Determines if this is boolean type.
     * @param clazz Class to be checked.
     * @return True if is boolean.
     */
    private static boolean isBooleanOrPrimitive(Class<?> clazz) {
        return (Boolean.class == clazz) || (boolean.class == clazz);
    }

    /**
     * Determines if this is byte type.
     * @param clazz Class to be checked.
     * @return True if is byte.
     */
    private static boolean isByteOrPrimitive(Class<?> clazz) {
        return (Byte.class == clazz) || (byte.class == clazz);
    }

    /**
     * Determines if this is short type.
     * @param clazz Class to be checked.
     * @return True if is short.
     */
    private static boolean isShortOrPrimitive(Class<?> clazz) {
        return (Short.class == clazz) || (short.class == clazz);
    }

    /**
     * Determines if this is integer type.
     * @param clazz Class to be checked.
     * @return True if is integer.
     */
    private static boolean isIntegerOrPrimitive(Class<?> clazz) {
        return (Integer.class == clazz) || (int.class == clazz);
    }

    /**
     * Determines if this is long type.
     * @param clazz Class to be checked.
     * @return True if is long.
     */
    private static boolean isLongOrPrimitive(Class<?> clazz) {
        return (Long.class == clazz) || (long.class == clazz);
    }

    /**
     * Determines if this is float type.
     * @param clazz Class to be checked.
     * @return True if is float.
     */
    private static boolean isFloatOrPrimitive(Class<?> clazz) {
        return (Float.class == clazz) || (float.class == clazz);
    }

    /**
     * Determines if this is double type.
     * @param clazz Class to be checked.
     * @return True if is double.
     */
    private static boolean isDoubleOrPrimitive(Class<?> clazz) {
        return (Double.class == clazz) || (double.class == clazz);
    }
}
