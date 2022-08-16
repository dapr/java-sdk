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

import io.dapr.client.domain.*;
import io.dapr.client.domain.query.Query;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprProtos;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class DaprPreviewClientHttpTest {
  private static final String CONFIG_STORE_NAME = "MyConfigStore";

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

  @Test
  public void getConfigurationTestErrorScenario() {
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.getConfiguration("", "key").block();
    });
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, null);
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.getConfiguration(req).block();
    });
  }

  @Test
  public void getSingleConfigurationTest() {
    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore")
            .param("key", "configkey1")
            .respond("[{\"key\":\"configkey1\", \"value\": \"configvalue1\", \"version\": \"1\"}]");

    ConfigurationItem ci = daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "configkey1").block();
    System.out.println(ci.getKey() + " <  bsdjbsj  >");
    assertEquals("configkey1", ci.getKey());
    assertEquals("configvalue1", ci.getValue());
    assertEquals("1", ci.getVersion());
  }

  @Test
  public void getMultipleConfigurationTest() {
    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore")
            .param("key", "configkey1")
            .respond("[{\"key\":\"configkey1\", \"value\": \"configvalue1\", \"version\": \"1\"}," +
                    "{\"key\":\"configkey2\", \"value\": \"configvalue2\", \"version\": \"1\"}]");

    List<ConfigurationItem> cis = daprPreviewClientHttp.getConfiguration(CONFIG_STORE_NAME, "configkey1","configkey2").block();
    assertEquals(2, cis.size());
    assertEquals("configkey1", cis.stream().findFirst().get().getKey());
    assertEquals("configvalue1", cis.stream().findFirst().get().getValue());
    assertEquals("1", cis.stream().findFirst().get().getVersion());

    assertEquals("configkey2", cis.stream().skip(1).findFirst().get().getKey());
    assertEquals("configvalue2", cis.stream().skip(1).findFirst().get().getValue());
    assertEquals("1", cis.stream().skip(1).findFirst().get().getVersion());
  }

  @Test
  public void subscribeConfigurationTest() {
    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore/subscribe")
            .param("key", "configkey1")
            .respond("{\"id\":\"1234\"}");

    Iterator<SubscribeConfigurationResponse> itr = daprPreviewClientHttp.subscribeConfiguration(CONFIG_STORE_NAME, "configkey1").toIterable().iterator();
    assertTrue(itr.hasNext());
    SubscribeConfigurationResponse res = itr.next();
    assertEquals("1234", res.getSubscriptionId());
    assertFalse(itr.hasNext());
  }

  @Test
  public void unsubscribeConfigurationTest() {
    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore/1234/unsubscribe")
            .respond(204);

    UnsubscribeConfigurationResponse res = daprPreviewClientHttp.unsubscribeConfiguration("1234", CONFIG_STORE_NAME).block();
    assertTrue(res.getIsUnsubscribed());
  }

  @Test
  public void unsubscribeConfigurationTestWithError() {
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.unsubscribeConfiguration("", CONFIG_STORE_NAME).block();
    });

    UnsubscribeConfigurationRequest req = new UnsubscribeConfigurationRequest("subscription_id", "");
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.unsubscribeConfiguration(req).block();
    });

    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore/1234/unsubscribe")
            .respond(500);
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.unsubscribeConfiguration("1234", CONFIG_STORE_NAME).block();
    });
  }

  @Test
  public void subscribeConfigurationTestWithError() {
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.subscribeConfiguration("", "key1").blockFirst();
    });

    List<String> list = new ArrayList<>();
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest("mystore", list);
    assertThrows(IllegalArgumentException.class, () -> {
      daprPreviewClientHttp.subscribeConfiguration(req).blockFirst();
    });

    mockInterceptor.addRule()
            .get()
            .path("/v1.0-alpha1/configuration/MyConfigStore/subscribe")
            .param("key", "configkey1")
            .respond(500);
    assertThrows(DaprException.class, () -> {
      daprPreviewClientHttp.subscribeConfiguration(CONFIG_STORE_NAME, "configkey1").blockFirst();
    });
  }
}
