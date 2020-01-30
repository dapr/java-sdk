/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.utils.DurationUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Serializes and deserializes internal objects.
 */
public class ObjectSerializer extends io.dapr.client.ObjectSerializer {

  /**
   * Shared Json Factory as per Jackson's documentation.
   */
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] serialize(Object state) throws IOException {
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
    return super.serialize(state);
  }


  /**
   * Faster serialization for params of Actor's timer.
   *
   * @param timer Timer's to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private byte[] serialize(ActorTimer<?> timer) throws IOException {
    if (timer == null) {
      return null;
    }

    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.convertDurationToDaprFormat(timer.getDueTime()));
      generator.writeStringField("period", DurationUtils.convertDurationToDaprFormat(timer.getPeriod()));
      generator.writeStringField("callback", timer.getCallback());
      if (timer.getState() != null) {
        generator.writeBinaryField("data", this.serialize(timer.getState()));
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toByteArray();
    }
  }

  /**
   * Faster serialization for Actor's reminder.
   *
   * @param reminder Reminder to be serialized.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private byte[] serialize(ActorReminderParams reminder) throws IOException {
    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.convertDurationToDaprFormat(reminder.getDueTime()));
      generator.writeStringField("period", DurationUtils.convertDurationToDaprFormat(reminder.getPeriod()));
      if (reminder.getData() != null) {
        generator.writeBinaryField("data", reminder.getData());
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toByteArray();
    }
  }

  /**
   * Faster serialization for Actor's runtime configuration.
   *
   * @param config Configuration for Dapr's actor runtime.
   * @return JSON String.
   * @throws IOException If cannot generate JSON.
   */
  private byte[] serialize(ActorRuntimeConfig config) throws IOException {
    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeArrayFieldStart("entities");
      for (String actorClass : config.getRegisteredActorTypes()) {
        generator.writeString(actorClass);
      }
      generator.writeEndArray();
      if (config.getActorIdleTimeout() != null) {
        generator.writeStringField("actorIdleTimeout",
            DurationUtils.convertDurationToDaprFormat(config.getActorIdleTimeout()));
      }
      if (config.getActorScanInterval() != null) {
        generator.writeStringField("actorScanInterval",
            DurationUtils.convertDurationToDaprFormat(config.getActorScanInterval()));
      }
      if (config.getDrainOngoingCallTimeout() != null) {
        generator.writeStringField("drainOngoingCallTimeout",
            DurationUtils.convertDurationToDaprFormat(config.getDrainOngoingCallTimeout()));
      }
      if (config.getDrainBalancedActors() != null) {
        generator.writeBooleanField("drainBalancedActors", config.getDrainBalancedActors());
      }
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toByteArray();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] content, Class<T> clazz) throws IOException {
    if (clazz == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return (T) deserializeActorReminder(content);
    }

    // Is not one of the special cases.
    return super.deserialize(content, clazz);
  }

  /**
   * Extracts the response data from a JSON Payload where data is in "data" attribute.
   *
   * @param payload JSON payload containing "data".
   * @return byte[] instance, null.
   * @throws IOException In case it cannot generate String.
   */
  public byte[] unwrapData(final byte[] payload) throws IOException {
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
  public byte[] wrapData(final byte[] data) throws IOException {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(output);
      generator.writeStartObject();
      if (data != null) {
        generator.writeBinaryField("data", data);
      }
      generator.writeEndObject();
      generator.close();
      output.flush();
      return output.toByteArray();
    }
  }

  /**
   * Deserializes an Actor Reminder.
   *
   * @param value Content to be deserialized.
   * @return Actor Reminder.
   * @throws IOException If cannot parse JSON.
   */
  private ActorReminderParams deserializeActorReminder(byte[] value) throws IOException {
    if (value == null) {
      return null;
    }

    JsonNode node = OBJECT_MAPPER.readTree(value);
    Duration dueTime = DurationUtils.convertDurationFromDaprFormat(node.get("dueTime").asText());
    Duration period = DurationUtils.convertDurationFromDaprFormat(node.get("period").asText());
    byte[] data = node.get("data") != null ? node.get("data").binaryValue() : null;

    return new ActorReminderParams(data, dueTime, period);
  }

}
