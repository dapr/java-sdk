/*
 * Copyright 2024 The Dapr Authors
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
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.MetadataEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Dapr Secrets API using testcontainers.
 */
@Disabled("Needs investigation: DaprContainer file mounting with secretstores.local.file")
@Testcontainers
@Tag("testcontainers")
public class DaprSecretsIT {

  private static final String SECRETS_STORE_NAME = "localSecretStore";
  private static final String CONTAINER_SECRETS_PATH = "/tmp/secrets.json";
  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

  private static final String KEY1 = "movie";
  private static final String KEY2 = "person";

  private static final Network DAPR_NETWORK = Network.newNetwork();

  private static DaprClient daprClient;

  private static final String SECRETS_JSON = createSecretsJson();

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("secrets-test-app")
      .withNetwork(DAPR_NETWORK)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withComponent(new Component(
          SECRETS_STORE_NAME,
          "secretstores.local.file",
          "v1",
          List.of(new MetadataEntry("secretsFile", CONTAINER_SECRETS_PATH))
      ))
      .withCopyToContainer(Transferable.of(SECRETS_JSON), CONTAINER_SECRETS_PATH)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

  private static String createSecretsJson() {
    try {
      Map<String, Object> secrets = new HashMap<>();
      Map<String, Object> movieSecret = new HashMap<>();
      movieSecret.put("title", "The Metrics IV");
      movieSecret.put("year", "2020");
      secrets.put(KEY1, movieSecret);

      Map<String, Object> personSecret = new HashMap<>();
      personSecret.put("name", "Jon Doe");
      secrets.put(KEY2, personSecret);

      return JSON_SERIALIZER.writeValueAsString(secrets);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  static void setUp() {
    daprClient = new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, DAPR_CONTAINER.getHttpEndpoint())
        .withPropertyOverride(Properties.GRPC_ENDPOINT, DAPR_CONTAINER.getGrpcEndpoint())
        .build();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (daprClient != null) {
      daprClient.close();
    }
  }

  @Test
  public void testGetSecret() {
    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();

    assertNotNull(data);
    assertEquals(2, data.size());
    assertEquals("The Metrics IV", data.get("title"));
    assertEquals("2020", data.get("year"));
  }

  @Test
  public void testGetBulkSecret() {
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
  public void testGetSecretKeyNotFound() {
    assertThrows(RuntimeException.class, () ->
        daprClient.getSecret(SECRETS_STORE_NAME, "unknownKey").block()
    );
  }

  @Test
  public void testGetSecretStoreNotFound() {
    assertThrows(RuntimeException.class, () ->
        daprClient.getSecret("unknownStore", "unknownKey").block()
    );
  }
}
