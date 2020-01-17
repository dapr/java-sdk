/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.utils.DurationUtils;
import io.dapr.utils.ObjectSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;

/**
 * Serializes and deserializes an object.
 */
public class ActorStateSerializer extends ObjectSerializer {

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> String serializeString(T state) throws IOException {
    if (state == null) {
      return null;
    }

    if (state.getClass() == ActorTimer.class) {
      // Special serializer for this internal classes.
      return serialize((ActorTimer) state);
    }

    if (state.getClass() == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return serialize((ActorReminderParams) state);
    }

    if (state.getClass() == ActorRuntimeConfig.class) {
      // Special serializer for this internal classes.
      return serialize((ActorRuntimeConfig) state);
    }

    // Is not an special case.
    return super.serializeString(state);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(Object value, Class<T> clazz) throws IOException {
    if (clazz == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return (T) deserializeActorReminder(value);
    }

    // Is not one of the special cases.
    return super.deserialize(value, clazz);
  }

  /**
   * Extracts the response data from a JSON Payload where data is in "data" attribute.
   *
   * @param payload JSON payload containing "data".
   * @return byte[] instance, null.
   * @throws IOException In case it cannot generate String.
   */
  public byte[] unwrapData(final String payload) throws IOException {
    if (payload == null) {
      return null;
    }

    JsonNode root = OBJECT_MAPPER.readTree(payload);
    if (root == null) {
      return null;
    }

    JsonNode dataNode = root.get("data");
    if (dataNode == null) {
      return null;
    }

    return dataNode.binaryValue();
  }

  /**
   * Wraps data in the "data" attribute in a JSON object.
   *
   * @param data bytes to be wrapped into the "data" attribute in a JSON object.
   * @return String to be sent to Dapr's API.
   * @throws RuntimeException In case it cannot generate String.
   */
  public String wrapData(final byte[] data) throws IOException {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      if (data != null) {
        generator.writeBinaryField("data", data);
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  /**
   * Faster serialization for params of Actor's timer.
   *
   * @param timer Timer's to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private String serialize(ActorTimer<?> timer) throws IOException {
    if (timer == null) {
      return null;
    }

    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.ConvertDurationToDaprFormat(timer.getDueTime()));
      generator.writeStringField("period", DurationUtils.ConvertDurationToDaprFormat(timer.getPeriod()));
      generator.writeStringField("callback", timer.getCallback());
      if (timer.getState() != null) {
        generator.writeStringField("data", this.serializeString(timer.getState()));
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  /**
   * Faster serialization for Actor's reminder.
   *
   * @param reminder Reminder to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private String serialize(ActorReminderParams reminder) throws IOException {
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
   * Faster serialization for Actor's runtime configuration.
   *
   * @param config Configuration for Dapr's actor runtime.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private String serialize(ActorRuntimeConfig config) throws IOException {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeArrayFieldStart("entities");
      for (String actorClass : config.getRegisteredActorTypes()) {
        generator.writeString(actorClass);
      }
      generator.writeEndArray();
      // TODO: handle configuration.
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  /**
   * Deserializes an Actor Reminder.
   *
   * @param value Content to be deserialized.
   * @return Actor Reminder.
   * @throws IOException If cannot parse JSON.
   */
  private ActorReminderParams deserializeActorReminder(Object value) throws IOException {
    if (value == null) {
      return null;
    }

    JsonNode node;
    if (value instanceof byte[]) {
      node = OBJECT_MAPPER.readTree((byte[]) value);
    } else {
      node = OBJECT_MAPPER.readTree(value.toString());
    }
    Duration dueTime = DurationUtils.ConvertDurationFromDaprFormat(node.get("dueTime").asText());
    Duration period = DurationUtils.ConvertDurationFromDaprFormat(node.get("period").asText());
    String data = node.get("data") != null ? node.get("data").asText() : null;

    return new ActorReminderParams(data, dueTime, period);
  }

}
