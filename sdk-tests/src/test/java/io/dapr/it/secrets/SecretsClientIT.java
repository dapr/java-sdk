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

package io.dapr.it.secrets;

import io.dapr.client.DaprClient;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Secrets Store APIs backed by redis secret store.
 */
@Testcontainers
@Tag("testcontainers")
public class SecretsClientIT {

  private static final String SECRETS_STORE_NAME = "localSecretStore";

  private static final String KEY1 = UUID.randomUUID().toString();

  private static final String KYE2 = UUID.randomUUID().toString();

  private static final Network NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("secrets-it")
      .withComponent(new Component(
          SECRETS_STORE_NAME,
          "secretstores.redis",
          "v1",
          Map.of("redisHost", "redis:6379", "redisPassword", "")));

  @BeforeAll
  public static void init() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(10000).block();
    }
  }

  @BeforeEach
  public void setup() throws Exception {
    REDIS.execInContainer("redis-cli", "DEL", KEY1);
    REDIS.execInContainer("redis-cli", "DEL", KYE2);
    REDIS.execInContainer("redis-cli", "HSET", KEY1, "title", "The Metrics IV", "year", "2020");
    REDIS.execInContainer("redis-cli", "HSET", KYE2, "name", "Jon Doe");
  }

  @Test
  public void getSecret() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();
      assertEquals(2, data.size());
      assertEquals("The Metrics IV", data.get("title"));
      assertEquals("2020", data.get("year"));
    }
  }

  @Test
  public void getBulkSecret() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      Map<String, Map<String, String>> data = daprClient.getBulkSecret(SECRETS_STORE_NAME).block();
      assertTrue(data.size() >= 2);
      assertEquals(2, data.get(KEY1).size());
      assertEquals("The Metrics IV", data.get(KEY1).get("title"));
      assertEquals("2020", data.get(KEY1).get("year"));
      assertEquals(1, data.get(KYE2).size());
      assertEquals("Jon Doe", data.get(KYE2).get("name"));
    }
  }

  @Test
  public void getSecretKeyNotFound() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      assertThrows(RuntimeException.class, () -> daprClient.getSecret(SECRETS_STORE_NAME, "unknownKey").block());
    }
  }

  @Test
  public void getSecretStoreNotFound() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      assertThrows(RuntimeException.class, () -> daprClient.getSecret("unknownStore", "unknownKey").block());
    }
  }
}
