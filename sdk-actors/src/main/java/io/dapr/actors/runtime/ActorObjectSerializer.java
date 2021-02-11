/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.client.ObjectSerializer;
import io.dapr.utils.DurationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Serializes and deserializes internal objects.
 */
public class ActorObjectSerializer extends ObjectSerializer {

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

    if (state.getClass() == ActorTimerParams.class) {
      // Special serializer for this internal classes.
      return serialize((ActorTimerParams) state);
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
  private byte[] serialize(ActorTimerParams timer) throws IOException {
    if (timer == null) {
      return null;
    }

    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeStringField("dueTime", DurationUtils.convertDurationToDaprFormat(timer.getDueTime()));
      generator.writeStringField("period", DurationUtils.convertDurationToDaprFormat(timer.getPeriod()));
      generator.writeStringField("callback", timer.getCallback());
      if (timer.getData() != null) {
        generator.writeBinaryField("data", timer.getData());
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
    if (clazz == ActorTimerParams.class) {
      // Special serializer for this internal classes.
      return (T) deserializeActorTimer(content);
    }

    if (clazz == ActorReminderParams.class) {
      // Special serializer for this internal classes.
      return (T) deserializeActorReminder(content);
    }

    // Is not one of the special cases.
    return super.deserialize(content, clazz);
  }

  /**
   * Deserializes an Actor Timer.
   *
   * @param value Content to be deserialized.
   * @return Actor Timer.
   * @throws IOException If cannot parse JSON.
   */
  private ActorTimerParams deserializeActorTimer(byte[] value) throws IOException {
    if (value == null) {
      return null;
    }

    JsonNode node = OBJECT_MAPPER.readTree(value);
    String callback = node.get("callback").asText();
    Duration dueTime = extractDurationOrNull(node, "dueTime");
    Duration period = extractDurationOrNull(node, "period");
    byte[] data = node.get("data") != null ? node.get("data").binaryValue() : null;

    return new ActorTimerParams(callback, data, dueTime, period);
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
    Duration dueTime = extractDurationOrNull(node, "dueTime");
    Duration period = extractDurationOrNull(node, "period");
    byte[] data = node.get("data") != null ? node.get("data").binaryValue() : null;

    return new ActorReminderParams(data, dueTime, period);
  }

  /**
   * Extracts duration or null.
   *
   * @param node Node that contains the attribute.
   * @param name Attribute name.
   * @return Parsed duration or null.
   */
  private static Duration extractDurationOrNull(JsonNode node, String name) {
    JsonNode valueNode = node.get(name);
    if (valueNode == null) {
      return  null;
    }

    return DurationUtils.convertDurationFromDaprFormat(valueNode.asText());
  }
}
