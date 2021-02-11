/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpProxy;
import io.dapr.config.Properties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.RuleAnswer;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

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
      .get("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/state/order")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<byte[]> mono = DaprHttpClient.getState("DemoActor", "1", "order");
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }


  @Test
  public void saveActorStateTransactionally() {
    mockInterceptor.addRule()
      .put("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/state")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    List<ActorStateOperation> ops = Collections.singletonList(new ActorStateOperation("UPSERT", "key", "value"));
    Mono<Void> mono = DaprHttpClient.saveStateTransactionally("DemoActor", "1", ops);
    assertNull(mono.block());
  }

  @Test
  public void registerActorReminder() {
    mockInterceptor.addRule()
      .put("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono =
      DaprHttpClient.registerReminder(
          "DemoActor",
          "1",
          "reminder",
          new ActorReminderParams("".getBytes(), Duration.ofSeconds(1), Duration.ofSeconds(2)));
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorReminder() {
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono = DaprHttpClient.unregisterReminder("DemoActor", "1", "reminder");
    assertNull(mono.block());
  }

  @Test
  public void registerActorTimer() {
    String data = "hello world";
    mockInterceptor.addRule()
      .put("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/timers/timer")
      .answer(new RuleAnswer() {
        @Override
        public Response.Builder respond(Request request) {
          String expectedBody = "{\"dueTime\":\"0h0m5s0ms\"," +
              "\"period\":\"0h0m10s0ms\"," +
              "\"callback\":\"mycallback\"," +
              "\"data\":\""+ Base64.getEncoder().encodeToString(data.getBytes()) +"\"}";
          String body = "";
          try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            body = buffer.readString(Charset.defaultCharset());
          } catch (IOException e) {
            fail();
          }
          assertEquals(expectedBody, body);
          return new Response.Builder()
              .code(200)
              .body(ResponseBody.create("{}", MediaType.get("application/json")));
        }
      });
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono =
      DaprHttpClient.registerTimer(
          "DemoActor",
          "1",
          "timer",
          new ActorTimerParams(
              "mycallback",
              data.getBytes(),
              Duration.ofSeconds(5),
              Duration.ofSeconds(10)));
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorTimer() {
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/actors/DemoActor/1/timers/timer")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttpProxy(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprHttpClient = new DaprHttpClient(daprHttp);
    Mono<Void> mono = DaprHttpClient.unregisterTimer("DemoActor", "1", "timer");
    assertNull(mono.block());
  }
}
