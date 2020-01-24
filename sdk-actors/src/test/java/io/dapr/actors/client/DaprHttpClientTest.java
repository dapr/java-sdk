/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpProxy;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class DaprHttpClientTest {

  private DaprHttpClient DaprHttpClient;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  private final String EXPECTED_RESULT = "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

  @Before
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
  }

  @Test
  public void invokeActorMethod() {
    DaprHttp daprHttpMock = mock(DaprHttp.class);
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/actors/DemoActor/1/method/Payment")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<byte[]> mono =
      DaprHttpClient.invokeActorMethod("DemoActor", "1", "Payment", "".getBytes());
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }

}