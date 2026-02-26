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

package io.dapr.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DropFailurePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FailurePolicySerializerTest {

  @Test
  public void constantPolicyTest()
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    var p = new ConstantFailurePolicy();
    p.setMaxRetries(3);
    p.setDurationBetweenRetries(Duration.ofSeconds(0));
    var json = mapper.writeValueAsString(p);

    assertEquals(
        "{\"maxRetries\":3,\"durationBetweenRetries\":0.0,\"failurePolicyType\":\"CONSTANT\"}",
        json);
  }

  @Test
  public void dropPolicyTest()
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    var p = new DropFailurePolicy();
    var json = mapper.writeValueAsString(p);

    assertEquals(
        "{\"failurePolicyType\":\"DROP\"}",
        json);
  }
}
