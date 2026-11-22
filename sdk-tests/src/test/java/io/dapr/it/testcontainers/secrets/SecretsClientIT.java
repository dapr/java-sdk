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
 * limitations under the License.
 */

package io.dapr.it.testcontainers.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.it.testcontainers.support.DaprTestEnvironment;
import io.dapr.testcontainers.Component;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for the local-file Dapr secret store without an application. */
@Tag("testcontainers")
public class SecretsClientIT {

  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();
  private static final String SECRETS_STORE_NAME = "localSecretStore";
  private static final String CONTAINER_SECRET_PATH = "/dapr-secret.json";
  private static final String KEY1 = UUID.randomUUID().toString();
  private static final String KEY2 = UUID.randomUUID().toString();

  private static final DaprTestEnvironment ENVIRONMENT = DaprTestEnvironment.withoutApplication("secrets-it");
  private DaprClient daprClient;

  @BeforeAll
  public static void init() throws Exception {
    byte[] secretJson = JSON_SERIALIZER.writeValueAsBytes(buildSecretPayload());

    ENVIRONMENT.withComponents(List.of(new Component(SECRETS_STORE_NAME, "secretstores.local.file", "v1", Map.of(
            "secretsFile", CONTAINER_SECRET_PATH,
            "nestedSeparator", ":",
            "multiValued", "true"
        ))))
        .withResource(Transferable.of(secretJson), CONTAINER_SECRET_PATH);
    ENVIRONMENT.start();
  }

  @AfterAll
  public static void stopEnvironment() {
    ENVIRONMENT.stop();
  }

  @BeforeEach
  public void setup() {
    this.daprClient = ENVIRONMENT.clientBuilder().build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (daprClient != null) {
      daprClient.close();
    }
  }

  @Test
  public void getSecret() {
    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();
    assertNotNull(data);
    assertEquals(2, data.size());
    assertEquals("The Metrics IV", data.get("title"));
    assertEquals("2020", data.get("year"));
  }

  @Test
  public void getBulkSecret() {
    Map<String, Map<String, String>> data = daprClient.getBulkSecret(SECRETS_STORE_NAME).block();
    assertNotNull(data);
    assertTrue(data.size() >= 2);
    assertEquals(2, data.get(KEY1).size());
    assertEquals("The Metrics IV", data.get(KEY1).get("title"));
    assertEquals("2020", data.get(KEY1).get("year"));
    assertEquals(1, data.get(KEY2).size());
    assertEquals("Jon Doe", data.get(KEY2).get("name"));
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
        KEY2, Map.of("name", "Jon Doe")
    );
  }
}
