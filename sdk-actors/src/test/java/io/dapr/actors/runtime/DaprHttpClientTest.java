/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpProxy;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DaprHttpClientTest {

  private DaprHttpClient DaprHttpClient;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  private final String EXPECTED_RESULT = "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

  @Before
  public void setUp() throws Exception {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
  }

  @Test
  public void getActorState() {
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/actors/DemoActor/1/state/order")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<byte[]> mono = DaprHttpClient.getActorState("DemoActor", "1", "order");
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }


  @Test
  public void saveActorStateTransactionally() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/state")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono =
      DaprHttpClient.saveActorStateTransactionally("DemoActor", "1", "".getBytes());
    assertNull(mono.block());
  }

  @Test
  public void registerActorReminder() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono =
      DaprHttpClient.registerActorReminder("DemoActor", "1", "reminder", "".getBytes());
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorReminder() {
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono = DaprHttpClient.unregisterActorReminder("DemoActor", "1", "reminder");
    assertNull(mono.block());
  }

  @Test
  public void registerActorTimer() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/timers/timer")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono =
      DaprHttpClient.registerActorTimer("DemoActor", "1", "timer", "".getBytes());
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorTimer() {
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/actors/DemoActor/1/timers/timer")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono = DaprHttpClient.unregisterActorTimer("DemoActor", "1", "timer");
    assertNull(mono.block());
  }

}