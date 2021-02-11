/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpProxy;
import io.dapr.config.Properties;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MediaTypes;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static io.dapr.actors.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertEquals;

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
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/method/Payment")
        .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<byte[]> mono =
        DaprHttpClient.invoke("DemoActor", "1", "Payment", "".getBytes());
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }

  @Test
  public void invokeActorMethodError() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/method/Payment")
        .respond(404,
            ResponseBody.create("" +
                "{\"errorCode\":\"ERR_SOMETHING\"," +
                "\"message\":\"error message\"}", MediaTypes.MEDIATYPE_JSON));
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<byte[]> mono =
        DaprHttpClient.invoke("DemoActor", "1", "Payment", "".getBytes());

    assertThrowsDaprException(
        "ERR_SOMETHING",
        "ERR_SOMETHING: error message",
        () -> mono.block());
  }

}
