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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Secrets Store APIs using local file.
 *
 * 1. create secret file locally:
 */
public class SecretsClientIT extends BaseIT {

  /**
   * JSON Serializer to print output.
   */
  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

  private static final String SECRETS_STORE_NAME = "localSecretStore";

  private static final String LOCAL_SECRET_FILE_PATH = "./components/secret.json";

  private static final String KEY1 = UUID.randomUUID().toString();

  private static final String KYE2 = UUID.randomUUID().toString();

  private static DaprRun daprRun;


  private DaprClient daprClient;

  private static File localSecretFile;

  @BeforeAll
  public static void init() throws Exception {

    localSecretFile = new File(LOCAL_SECRET_FILE_PATH);
    boolean existed = localSecretFile.exists();
    assertTrue(existed);
    initSecretFile();

    daprRun = startDaprApp(SecretsClientIT.class.getSimpleName(), 5000);
  }

  @BeforeEach
  public void setup() {
    this.daprClient = new DaprClientBuilder().build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    daprClient.close();
    clearSecretFile();
  }

  @Test
  public void getSecret() throws Exception {
    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, KEY1).block();
    assertEquals(2, data.size());
    assertEquals("The Metrics IV", data.get("title"));
    assertEquals("2020", data.get("year"));
  }

  @Test
  public void getBulkSecret() throws Exception {
    Map<String, Map<String, String>> data = daprClient.getBulkSecret(SECRETS_STORE_NAME).block();
    // There can be other keys from other runs or test cases, so we are good with at least two.
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

  private static void initSecretFile() throws Exception {
    Map<String, Object> key2 = new HashMap(){{
      put("name", "Jon Doe");
    }};
    Map<String, Object> key1 = new HashMap(){{
      put("title", "The Metrics IV");
      put("year", "2020");
    }};
    Map<String, Map<String, Object>> secret = new HashMap<>(){{
      put(KEY1, key1);
      put(KYE2, key2);
    }};
    try (FileOutputStream fos = new FileOutputStream(localSecretFile)) {
      JSON_SERIALIZER.writeValue(fos, secret);
    }
  }

  private static void clearSecretFile() throws IOException {
    try (FileOutputStream fos = new FileOutputStream(localSecretFile)) {
      IOUtils.write("{}", fos);
    }
  }
}
