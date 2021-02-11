/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import okhttp3.OkHttpClient;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class DaprHttpBuilderTest {

  @Test
  public void singletonOkHttpClient() throws Exception {
    DaprHttp daprHttp = new DaprHttpBuilder().build();
    DaprHttp anotherDaprHttp = new DaprHttpBuilder().build();

    assertSame(getOkHttpClient(daprHttp), getOkHttpClient(anotherDaprHttp));
  }


  private static OkHttpClient getOkHttpClient(DaprHttp daprHttp) throws Exception {
    Field httpClientField = DaprHttp.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    OkHttpClient okHttpClient = (OkHttpClient) httpClientField.get(daprHttp);
    assertNotNull(okHttpClient);
    return okHttpClient;
  }

}
