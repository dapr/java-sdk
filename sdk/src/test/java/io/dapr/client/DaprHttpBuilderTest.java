/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import okhttp3.OkHttpClient;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DaprHttpBuilderTest {

  @Test
  public void withReadTimeout() throws Exception {
    DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();
    Duration duration = Duration.ofSeconds(999);
    daprHttpBuilder.build();
    DaprHttpBuilder dapr = daprHttpBuilder.withReadTimeout(duration);
    assertNotNull(dapr);

    DaprHttp daprHttp = daprHttpBuilder.build();
    assertOKHttpPropertyValue(daprHttp, "readTimeoutMillis", (int)duration.toMillis());
  }

  private static void assertOKHttpPropertyValue(DaprHttp daprHttp, String propertyName, Object expectedValue) throws Exception {
    // First, get okHttpClient.
    Field httpClientField = DaprHttp.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    OkHttpClient okHttpClient = (OkHttpClient) httpClientField.get(daprHttp);
    assertNotNull(okHttpClient);

    Field propertyField = OkHttpClient.class.getDeclaredField(propertyName);
    propertyField.setAccessible(true);
    Object value = propertyField.get(okHttpClient);
    assertNotNull(value);
    assertEquals(expectedValue, value);
  }

}