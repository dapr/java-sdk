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

package io.dapr.client;

import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.query.Query;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.TypeRef;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DaprPreviewClientHttpTest {
  private static final String CONFIG_STORE_NAME = "MyConfigurationStore";

  private DaprPreviewClient daprPreviewClientHttp;

  private DaprHttp daprHttp;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  @Before
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprPreviewClientHttp = new DaprClientHttp(daprHttp);
  }

  @Test
  public void getConfigurationWithSingleKey() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "key").block();
    });
  }

  @Test
  public void getConfiguration() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "key1", "key2").block();
    });
  }

  @Test
  public void subscribeConfigurations() {
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.subscribeToConfiguration(CONFIG_STORE_NAME, "key1", "key2").blockFirst();
    });
  }

  @Test
  public void queryStateExceptionsTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("", "query", TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("", "query", String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", "", TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", "", String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", (Query) null, TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", (Query) null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", (String) null, TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState("storeName", (String) null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState(null, TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState(new QueryStateRequest("storeName"), TypeRef.BOOLEAN).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.queryState(null, String.class).block();
    });
  }

  @Test
  public void queryStateTest() {
    mockInterceptor.addRule()
        .post()
        .path("/v1.0-alpha1/state/testStore/query")
        .respond("{\"results\": [{\"key\": \"1\",\"data\": \"testData\","
            + "\"etag\": \"6f54ad94-dfb9-46f0-a371-e42d550adb7d\"}]}");
    QueryStateResponse<String> response = daprPreviewClientHttp.queryState("testStore", "query", String.class).block();
    assertNotNull(response);
    assertEquals("result size must be 1", 1, response.getResults().size());
    assertEquals("result must be same", "1", response.getResults().get(0).getKey());
    assertEquals("result must be same", "testData", response.getResults().get(0).getValue());
    assertEquals("result must be same", "6f54ad94-dfb9-46f0-a371-e42d550adb7d", response.getResults().get(0).getEtag());
  }
}
