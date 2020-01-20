/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;

import java.io.IOException;

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

        // Have this check here to be consistent with deserialization (see deserialize() method below).
        if (state instanceof byte[]) {
            return (byte[]) state;
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.writeValueAsBytes(state);
    }

    /**
     * Deserializes the byte array into the original object.
     *
     * @param content Content to be parsed.
     * @param clazz Type of the object being deserialized.
     * @param <T>   Generic type of the object being deserialized.
     * @return Object of type T.
     * @throws IOException In case content cannot be deserialized.
     */
    public <T> T deserialize(byte[] content, Class<T> clazz) throws IOException {
        if (clazz == null) {
            return null;
        }

        if (clazz.isPrimitive()) {
            return deserializePrimitives(content, clazz);
        }

        if ((content == null) || (content.length == 0)) {
            return (T) null;
        }

        // Deserialization of GRPC response fails without this check since it does not come as base64 encoded byte[].
        if (clazz == byte[].class) {
            return (T) content;
        }

        if (clazz == CloudEvent.class) {
            return (T) CloudEvent.deserialize(content);
        }

        return OBJECT_MAPPER.readValue(content, clazz);
    }

    /**
     * Parses a given String to the corresponding object defined by class.
     *
     * @param content Value to be parsed.
     * @param clazz Class of the expected result type.
     * @param <T>   Result type.
     * @return Result as corresponding type.
     * @throws Exception if cannot deserialize primitive time.
     */
    private static <T> T deserializePrimitives(byte[] content, Class<T> clazz) throws IOException {
        if ((content == null) || (content.length == 0)) {
            if (boolean.class == clazz) return (T) Boolean.FALSE;
            if (byte.class == clazz) return (T) Byte.valueOf((byte) 0);
            if (short.class == clazz) return (T) Short.valueOf((short) 0);
            if (int.class == clazz) return (T) Integer.valueOf(0);
            if (long.class == clazz) return (T) Long.valueOf(0L);
            if (float.class == clazz) return (T) Float.valueOf(0);
            if (double.class == clazz) return (T) Double.valueOf(0);
            if (char.class == clazz) return (T) Character.valueOf(Character.MIN_VALUE);

            return null;
        }

        return OBJECT_MAPPER.readValue(content, clazz);
    }
}
