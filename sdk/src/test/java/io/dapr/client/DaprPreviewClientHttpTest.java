/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.Assert.assertNull;
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
}
