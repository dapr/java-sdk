/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;

/**
 * Serializes and deserializes an object.
 */
class ObjectSerializer {

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

    if (state.getClass() == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return serialize((ActorReminderParams) state);
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
   * @param value String to be parsed.
   * @param clazz Type of the object being deserialized.
   * @param <T>   Generic type of the object being deserialized.
   * @return Object of type T.
   * @throws IOException
   */
  <T> T deserialize(String value, Class<T> clazz) throws IOException {
    if (clazz == String.class) {
      return (T) value;
    }

    if (clazz == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return (T) deserializeActorReminder(value);
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

  /**
   * Checks if the class is a primitive or equivalent.
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
   * @param value String to be parsed.
   * @param clazz Class of the expected result type.
   * @param <T> Result type.
   * @return Result as corresponding type.
   */
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

  /**
   * Faster serialization for Actor's timer.
   * @param timer Timer to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private static String serialize(ActorTimer<?> timer) throws IOException {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.ConvertDurationToDaprFormat(timer.getDueTime()));
      generator.writeStringField("period", DurationUtils.ConvertDurationToDaprFormat(timer.getPeriod()));
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  /**
   * Faster serialization for Actor's reminder.
   * @param reminder Reminder to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private static String serialize(ActorReminderParams reminder) throws IOException {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.ConvertDurationToDaprFormat(reminder.getDueTime()));
      generator.writeStringField("period", DurationUtils.ConvertDurationToDaprFormat(reminder.getPeriod()));
      if (reminder.getData() != null) {
        generator.writeStringField("data", reminder.getData());
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  /**
   * Deserializes an Actor Reminder.
   * @param value String to be deserialized.
   * @return Actor Reminder.
   * @throws IOException If cannot parse JSON.
   */
  private static ActorReminderParams deserializeActorReminder(String value) throws IOException {
    if (value == null) {
      return null;
    }

    JsonNode node = OBJECT_MAPPER.readTree(value);
    Duration dueTime = DurationUtils.ConvertDurationFromDaprFormat(node.get("dueTime").asText());
    Duration period = DurationUtils.ConvertDurationFromDaprFormat(node.get("period").asText());
    String data = node.get("data") != null ? node.get("data").asText() : null;

    return new ActorReminderParams(data, dueTime, period);
  }
}
