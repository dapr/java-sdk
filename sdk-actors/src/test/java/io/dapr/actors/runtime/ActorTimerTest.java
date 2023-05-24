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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.utils.RepeatedDuration;
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

    Duration ttl = Duration.ZERO
      .plusHours(4)
      .plusSeconds(2);

    ActorTimerParams timer = new ActorTimerParams(
      "myfunction",
      null,
      dueTime,
      new RepeatedDuration(period),
      new RepeatedDuration(ttl, 4));
    byte[] s = new ActorObjectSerializer().serialize(timer);

    String expected = "{\"period\":\"PT1H3S\",\"dueTime\":\"0h7m17s0ms\", \"callback\": \"myfunction\", \"ttl\": \"R4/PT4H2S\"}";
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
