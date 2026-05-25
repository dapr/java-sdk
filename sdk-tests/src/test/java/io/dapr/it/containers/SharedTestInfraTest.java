/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.it.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedTestInfraTest {

  @Test
  void networkIsSingleton() {
    Network n1 = SharedTestInfra.network();
    Network n2 = SharedTestInfra.network();
    assertSame(n1, n2);
  }

  @Test
  void redisStartsAndIsReachable() {
    GenericContainer<?> redis = SharedTestInfra.redis();
    assertTrue(redis.isRunning());
    assertNotNull(redis.getMappedPort(6379));
    assertEquals("redis", redis.getNetworkAliases().get(0));
  }

  @Test
  void redisInternalHostFormat() {
    SharedTestInfra.redis();  // ensure started
    assertEquals("redis:6379", SharedTestInfra.redisInternalHost());
  }

  @Test
  void zipkinStartsAndIsReachable() {
    GenericContainer<?> z = SharedTestInfra.zipkin();
    assertTrue(z.isRunning());
    assertNotNull(z.getMappedPort(9411));
    assertEquals("zipkin", z.getNetworkAliases().get(0));
  }

  @Test
  void zipkinInternalEndpointFormat() {
    SharedTestInfra.zipkin();   // ensure started
    assertEquals("http://zipkin:9411/api/v2/spans", SharedTestInfra.zipkinInternalEndpoint());
  }
}
