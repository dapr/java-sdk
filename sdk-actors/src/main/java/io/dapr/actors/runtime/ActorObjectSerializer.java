/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.client.ObjectSerializer;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DropFailurePolicy;
import io.dapr.client.domain.FailurePolicy;
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
      if (reminder.getFailurePolicy() != null) {
        generator.writeFieldName("failurePolicy");
        generator.writeStartObject();

        // Add type information for polymorphic deserialization
        if (reminder.getFailurePolicy() instanceof DropFailurePolicy) {
          generator.writeStringField("failurePolicyType", "DROP");
        } else if (reminder.getFailurePolicy() instanceof ConstantFailurePolicy) {
          generator.writeStringField("failurePolicyType", "CONSTANT");
          ConstantFailurePolicy constantPolicy = (ConstantFailurePolicy) reminder.getFailurePolicy();
          if (constantPolicy.getMaxRetries() != null) {
            generator.writeNumberField("maxRetries", constantPolicy.getMaxRetries());
          }
          if (constantPolicy.getDurationBetweenRetries() != null) {
            generator.writeStringField("interval",
                DurationUtils.convertDurationToDaprFormat(constantPolicy.getDurationBetweenRetries()));
          }
        }

        generator.writeEndObject();
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
      if (config.getRemindersStoragePartitions() != null) {
        generator.writeNumberField("remindersStoragePartitions", config.getRemindersStoragePartitions());
      }
      if (!config.getActorTypeConfigs().isEmpty()) {

        generator.writeArrayFieldStart("entitiesConfig");
        for (ActorTypeConfig actorTypeConfig : config.getActorTypeConfigs()) {
          generator.writeStartObject();
          if (actorTypeConfig.getActorTypeName() != null) {
            generator.writeArrayFieldStart("entities");
            generator.writeString(actorTypeConfig.getActorTypeName());
            generator.writeEndArray();
          }
          if (actorTypeConfig.getActorIdleTimeout() != null) {
            generator.writeStringField("actorIdleTimeout",
                DurationUtils.convertDurationToDaprFormat(actorTypeConfig.getActorIdleTimeout()));
          }
          if (actorTypeConfig.getActorScanInterval() != null) {
            generator.writeStringField("actorScanInterval",
                DurationUtils.convertDurationToDaprFormat(actorTypeConfig.getActorScanInterval()));
          }
          if (actorTypeConfig.getDrainOngoingCallTimeout() != null) {
            generator.writeStringField("drainOngoingCallTimeout",
                DurationUtils.convertDurationToDaprFormat(actorTypeConfig.getDrainOngoingCallTimeout()));
          }
          if (actorTypeConfig.getDrainBalancedActors() != null) {
            generator.writeBooleanField("drainBalancedActors", actorTypeConfig.getDrainBalancedActors());
          }
          if (actorTypeConfig.getRemindersStoragePartitions() != null) {
            generator.writeNumberField("remindersStoragePartitions", actorTypeConfig.getRemindersStoragePartitions());
          }

          generator.writeEndObject();
        }
        generator.writeEndArray();
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

    // Handle failure policy if present
    JsonNode failurePolicyNode = node.get("failurePolicy");
    FailurePolicy failurePolicy = null;
    if (failurePolicyNode != null) {
      // Try to determine the type based on the JSON structure
      if (failurePolicyNode.has("failurePolicyType")) {
        String policyType = failurePolicyNode.get("failurePolicyType").asText();
        if ("DROP".equals(policyType)) {
          failurePolicy = new DropFailurePolicy();
        } else if ("CONSTANT".equals(policyType)) {
          if (failurePolicyNode.has("maxRetries")) {
            failurePolicy = new ConstantFailurePolicy(failurePolicyNode.get("maxRetries").asInt());
          }
          if (failurePolicyNode.has("interval")) {
            failurePolicy = new ConstantFailurePolicy(DurationUtils
                .convertDurationFromDaprFormat(failurePolicyNode.get("interval").asText()));
          }
        }
      }
    }

    return new ActorReminderParams(data, dueTime, period, failurePolicy);
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
      return null;
    }

    return DurationUtils.convertDurationFromDaprFormat(valueNode.asText());
  }
}
