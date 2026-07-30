/*
 * Copyright 2026 The Dapr Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Unit tests for {@link ActorObjectSerializer}.
 */
public class ActorObjectSerializerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final ActorObjectSerializer SERIALIZER = new ActorObjectSerializer();

  public static class CustomState {

    public String name;

    public int count;

    public CustomState() {
    }

    public CustomState(String name, int count) {
      this.name = name;
      this.count = count;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof CustomState)) {
        return false;
      }
      CustomState other = (CustomState) obj;
      return this.count == other.count && Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.count);
    }
  }

  @Test
  public void serializeNullReturnsNull() throws IOException {
    Assertions.assertNull(SERIALIZER.serialize(null));
  }

  @Test
  public void deserializeNullActorTimerReturnsNull() throws IOException {
    Assertions.assertNull(SERIALIZER.deserialize(null, ActorTimerParams.class));
  }

  @Test
  public void deserializeNullActorReminderReturnsNull() throws IOException {
    Assertions.assertNull(SERIALIZER.deserialize(null, ActorReminderParams.class));
  }

  @Test
  public void serializeActorTimerWithData() throws IOException {
    byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
    ActorTimerParams timer = new ActorTimerParams(
        "myCallback",
        data,
        Duration.ofMinutes(1),
        Duration.ofMinutes(2));

    byte[] serialized = SERIALIZER.serialize(timer);

    String expected = "{\"dueTime\":\"0h1m0s0ms\",\"period\":\"0h2m0s0ms\","
        + "\"callback\":\"myCallback\",\"data\":\"aGVsbG8gd29ybGQ=\"}";
    Assertions.assertEquals(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(serialized));
  }

  @Test
  public void actorTimerRoundTrip() throws IOException {
    byte[] data = "timer state".getBytes(StandardCharsets.UTF_8);
    ActorTimerParams original = new ActorTimerParams(
        "myCallback",
        data,
        Duration.ofMinutes(7).plusSeconds(17),
        Duration.ofHours(1).plusSeconds(3));

    byte[] serialized = SERIALIZER.serialize(original);
    ActorTimerParams recreated = SERIALIZER.deserialize(serialized, ActorTimerParams.class);

    Assertions.assertEquals(original.getCallback(), recreated.getCallback());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
    Assertions.assertArrayEquals(original.getData(), recreated.getData());
  }

  @Test
  public void actorTimerRoundTripWithoutData() throws IOException {
    ActorTimerParams original = new ActorTimerParams(
        "myCallback",
        null,
        Duration.ofSeconds(30),
        Duration.ofMinutes(5));

    byte[] serialized = SERIALIZER.serialize(original);
    ActorTimerParams recreated = SERIALIZER.deserialize(serialized, ActorTimerParams.class);

    Assertions.assertEquals(original.getCallback(), recreated.getCallback());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
    Assertions.assertNull(recreated.getData());
  }

  @Test
  public void deserializeActorTimerWithoutOptionalAttributes() throws IOException {
    byte[] serialized = "{\"callback\":\"myCallback\"}".getBytes(StandardCharsets.UTF_8);

    ActorTimerParams recreated = SERIALIZER.deserialize(serialized, ActorTimerParams.class);

    Assertions.assertEquals("myCallback", recreated.getCallback());
    Assertions.assertNull(recreated.getDueTime());
    Assertions.assertNull(recreated.getPeriod());
    Assertions.assertNull(recreated.getData());
  }

  @Test
  public void serializeActorRuntimeConfigWithAllAttributes() throws IOException {
    ActorRuntimeConfig config = new ActorRuntimeConfig()
        .addRegisteredActorType("MyActor")
        .setActorIdleTimeout(Duration.ofHours(1))
        .setActorScanInterval(Duration.ofSeconds(30))
        .setDrainOngoingCallTimeout(Duration.ofMinutes(1))
        .setDrainBalancedActors(true)
        .setRemindersStoragePartitions(7);

    byte[] serialized = SERIALIZER.serialize(config);

    String expected = "{\"entities\":[\"MyActor\"],"
        + "\"actorIdleTimeout\":\"1h0m0s0ms\","
        + "\"actorScanInterval\":\"0h0m30s0ms\","
        + "\"drainOngoingCallTimeout\":\"0h1m0s0ms\","
        + "\"drainBalancedActors\":true,"
        + "\"remindersStoragePartitions\":7}";
    Assertions.assertEquals(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(serialized));
  }

  @Test
  public void serializeActorRuntimeConfigWithActorTypeConfig() throws IOException {
    ActorTypeConfig actorTypeConfig = new ActorTypeConfig()
        .setActorTypeName("MyActor")
        .setActorIdleTimeout(Duration.ofHours(2))
        .setDrainBalancedActors(false)
        .setRemindersStoragePartitions(3);
    ActorRuntimeConfig config = new ActorRuntimeConfig()
        .addRegisteredActorType("MyActor")
        .addActorTypeConfig(actorTypeConfig);

    byte[] serialized = SERIALIZER.serialize(config);

    String expected = "{\"entities\":[\"MyActor\"],"
        + "\"entitiesConfig\":[{"
        + "\"entities\":[\"MyActor\"],"
        + "\"actorIdleTimeout\":\"2h0m0s0ms\","
        + "\"drainBalancedActors\":false,"
        + "\"remindersStoragePartitions\":3}]}";
    Assertions.assertEquals(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(serialized));
  }

  @Test
  public void customTypeRoundTripUsesDefaultSerialization() throws IOException {
    CustomState original = new CustomState("myState", 42);

    byte[] serialized = SERIALIZER.serialize(original);
    CustomState recreated = SERIALIZER.deserialize(serialized, CustomState.class);

    Assertions.assertEquals(original, recreated);
  }
}
