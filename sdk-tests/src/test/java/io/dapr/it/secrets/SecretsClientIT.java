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

package io.dapr.it.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecretsClientIT extends BaseContainerIT {

  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();
  private static final String SECRETS_STORE_NAME = "localSecretStore";
  private static final String CONTAINER_SECRET_PATH = "/dapr-secret.json";
  private static final String KEY1 = UUID.randomUUID().toString();
  private static final String KYE2 = UUID.randomUUID().toString();

  private static DaprContainer dapr;
  private DaprClient daprClient;

  @BeforeAll
  public static void init() throws Exception {
    byte[] secretJson = JSON_SERIALIZER.writeValueAsBytes(buildSecretPayload());

    dapr = daprBuilder("secrets-it")
        .withComponent(new Component(SECRETS_STORE_NAME, "secretstores.local.file", "v1", Map.of(
            "secretsFile", CONTAINER_SECRET_PATH,
            "nestedSeparator", ":",
            "multiValued", "true"
        )))
        .withCopyToContainer(Transferable.of(secretJson), CONTAINER_SECRET_PATH);
    dapr.start();
    deferStop(dapr);
  }

  @BeforeEach
  public void setup() {
    this.daprClient = newDaprClient(dapr);
  }

  @AfterEach
  public void tearDown() throws Exception {
    daprClient.close();
  }

  @Test
  public void getSecret() {
    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();
    assertEquals(2, data.size());
    assertEquals("The Metrics IV", data.get("title"));
    assertEquals("2020", data.get("year"));
  }

  @Test
  public void getBulkSecret() {
    Map<String, Map<String, String>> data = daprClient.getBulkSecret(SECRETS_STORE_NAME).block();
    assertTrue(data.size() >= 2);
    assertEquals(2, data.get(KEY1).size());
    assertEquals("The Metrics IV", data.get(KEY1).get("title"));
    assertEquals("2020", data.get(KEY1).get("year"));
    assertEquals(1, data.get(KYE2).size());
    assertEquals("Jon Doe", data.get(KYE2).get("name"));
  }

  @Test
  public void getSecretKeyNotFound() {
    assertThrows(RuntimeException.class, () -> daprClient.getSecret(SECRETS_STORE_NAME, "unknownKey").block());
  }

  @Test
  public void getSecretStoreNotFound() {
    assertThrows(RuntimeException.class, () -> daprClient.getSecret("unknownStore", "unknownKey").block());
  }

  private static Map<String, Map<String, Object>> buildSecretPayload() {
    return Map.of(
        KEY1, Map.of("title", "The Metrics IV", "year", "2020"),
        KYE2, Map.of("name", "Jon Doe")
    );
  }
}
