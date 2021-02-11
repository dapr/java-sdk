/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

public class ActorTimerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void serialize() throws IOException {
    Duration dueTime = Duration.ZERO
      .plusMinutes(7)
      .plusSeconds(17);

    Duration period = Duration.ZERO
      .plusHours(1)
      .plusSeconds(3);

    ActorTimerParams timer = new ActorTimerParams(
      "myfunction",
      null,
      dueTime,
      period);
    byte[] s = new ActorObjectSerializer().serialize(timer);

    String expected = "{\"period\":\"1h0m3s0ms\",\"dueTime\":\"0h7m17s0ms\", \"callback\": \"myfunction\"}";
    // Deep comparison via JsonNode.equals method.
    Assert.assertEquals(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(s));
  }

  @Test
  public void serializeWithOneTimePeriod() throws IOException {
    Duration dueTime = Duration.ZERO
      .plusMinutes(7)
      .plusSeconds(17);

    // this is intentionally negative
    Duration period = Duration.ZERO
      .minusHours(1)
      .minusMinutes(3);

    ActorTimerParams timer = new ActorTimerParams(
      "myfunction",
      null,
      dueTime,
      period);
    byte[] s = new ActorObjectSerializer().serialize(timer);

    // A negative period will be serialized to an empty string which is interpreted by Dapr to mean fire once only.
    String expected = "{\"period\":\"\",\"dueTime\":\"0h7m17s0ms\", \"callback\": \"myfunction\"}";
    // Deep comparison via JsonNode.equals method.
    Assert.assertEquals(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(s));
  }
}
