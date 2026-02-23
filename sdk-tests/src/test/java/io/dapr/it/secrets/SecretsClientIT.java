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

import io.dapr.it.testcontainers.TestContainerNetworks;
import io.dapr.client.DaprClient;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Secrets Store APIs backed by local file secret store.
 */
@Testcontainers
@Tag("testcontainers")
public class SecretsClientIT {

  private static final String SECRETS_STORE_NAME = "localSecretStore";
  private static final String SECRET_FILE_PATH = "/dapr-resources/secrets.json";

  private static final String KEY1 = "metrics";
  private static final String KEY2 = "person";
  private static final String SECRET_FILE_CONTENT = "{\n"
      + "  \"" + KEY1 + "\": {\n"
      + "    \"title\": \"The Metrics IV\",\n"
      + "    \"year\": \"2020\"\n"
      + "  },\n"
      + "  \"" + KEY2 + "\": {\n"
      + "    \"name\": \"Jon Doe\"\n"
      + "  }\n"
      + "}\n";

  private static final Network NETWORK = TestContainerNetworks.STATE_NETWORK;

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("secrets-it")
      .withCopyToContainer(Transferable.of(SECRET_FILE_CONTENT), SECRET_FILE_PATH)
      .withComponent(new Component(
          SECRETS_STORE_NAME,
          "secretstores.local.file",
          "v1",
          Map.of("secretsFile", SECRET_FILE_PATH, "multiValued", "true")));

  @BeforeAll
  public static void init() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(10000).block();
    }
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
      assertEquals(1, data.get(KEY2).size());
      assertEquals("Jon Doe", data.get(KEY2).get("name"));
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
