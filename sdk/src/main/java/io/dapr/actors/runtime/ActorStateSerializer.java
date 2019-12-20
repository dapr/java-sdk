/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Serializes and deserializes an object.
 */
class ActorStateSerializer {

    /**
     * Shared Json Factory as per Jackson's documentation, used only for this class.
     */
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

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

        if (state.getClass() == ActorTimer.class) {
            // Special serializer for this internal classes.
            return serialize((ActorTimer<?>) state);
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

        if (isPrimitiveOrEquivalent(clazz)) {
            return parse(value, clazz);
        }

        if (value == null) {
            return (T) null;
        }

        // Not string, not primitive, so it is a complex type: we use JSON for that.
        return OBJECT_MAPPER.readValue(value, clazz);
    }

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

    private static <T> T parse(String value, Class<T> clazz) {
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

    private static String serialize(ActorTimer<?> timer) throws IOException {
        try (Writer writer = new StringWriter()) {
            JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
            generator.writeStartObject();
            generator.writeStringField("dueTime", ConverterUtils.ConvertDurationToDaprFormat(timer.getDueTime()));
            generator.writeStringField("period", ConverterUtils.ConvertDurationToDaprFormat(timer.getPeriod()));
            generator.writeEndObject();
            generator.close();
            writer.flush();
            return writer.toString();
        }
    }
}
