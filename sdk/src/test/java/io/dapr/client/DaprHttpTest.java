/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import reactor.util.context.Context;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


public class DaprHttpTest {

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private static final String STATE_PATH = DaprHttp.API_VERSION + "/state";

  private static final String EXPECTED_RESULT =
      "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  private ObjectSerializer serializer = new ObjectSerializer();

  @Before
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
  }

  @Test
  public void invokeApi_daprApiToken_present() throws IOException {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3500/v1.0/state")
        .hasHeader(Headers.DAPR_API_TOKEN)
        .respond(serializer.serialize(EXPECTED_RESULT));
    environmentVariables.set(Properties.API_TOKEN.getEnvName(), "xyz");
    assertEquals("xyz", Properties.API_TOKEN.get());
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, (byte[]) null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokeApi_daprApiToken_absent() throws IOException {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3500/v1.0/state")
        .not()
        .hasHeader(Headers.DAPR_API_TOKEN)
        .respond(serializer.serialize(EXPECTED_RESULT));
    assertNull(Properties.API_TOKEN.get());
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, (byte[]) null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokeMethod() throws IOException {
    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "text/html");
    headers.put("header1", "value1");
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3500/v1.0/state")
        .respond(serializer.serialize(EXPECTED_RESULT));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, (byte[]) null, headers, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokePostMethod() throws IOException {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3500/v1.0/state")
      .respond(serializer.serialize(EXPECTED_RESULT))
      .addHeader("Header", "Value");
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, "", null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokeDeleteMethod() throws IOException {
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3500/v1.0/state")
      .respond(serializer.serialize(EXPECTED_RESULT));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("DELETE", "v1.0/state".split("/"), null, (String) null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokeGetMethod() throws IOException {
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3500/v1.0/get")
      .respond(serializer.serialize(EXPECTED_RESULT));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi("GET", "v1.0/get".split("/"), null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test
  public void invokeMethodWithHeaders() throws IOException {
    Map<String, String> headers = new HashMap<>();
    headers.put("header", "value");
    headers.put("header1", "value1");
    Map<String, List<String>> urlParameters = new HashMap<>();
    urlParameters.put("orderId", Collections.singletonList("41"));
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3500/v1.0/state/order?orderId=41")
      .respond(serializer.serialize(EXPECTED_RESULT));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("GET", "v1.0/state/order".split("/"), urlParameters, headers, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test(expected = RuntimeException.class)
  public void invokePostMethodRuntime() throws IOException {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3500/v1.0/state")
      .respond(500);
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono =
        daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test(expected = RuntimeException.class)
  public void invokePostDaprError() throws IOException {

    mockInterceptor.addRule()
      .post("http://127.0.0.1:3500/v1.0/state")
      .respond(500, ResponseBody.create(MediaType.parse("text"),
        "{\"errorCode\":null,\"message\":null}"));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  @Test(expected = RuntimeException.class)
  public void invokePostMethodUnknownError() throws IOException {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3500/v1.0/state")
      .respond(500, ResponseBody.create(MediaType.parse("application/json"),
        "{\"errorCode\":\"null\",\"message\":\"null\"}"));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> mono = daprHttp.invokeApi("POST", "v1.0/state".split("/"), null, null, Context.empty());
    DaprHttp.Response response = mono.block();
    String body = serializer.deserialize(response.getBody(), String.class);
    assertEquals(EXPECTED_RESULT, body);
  }

  /**
   * The purpose of this test is to show that it doesn't matter when the client is called, the actual coll to DAPR
   * will be done when the output Mono response call the Mono.block method.
   * Like for instanche if you call getState, withouth blocking for the response, and then call delete for the same state
   * you just retrived but block for the delete response, when later you block for the response of the getState, you will
   * not found the state.
   * <p>This test will execute the following flow:</p>
   * <ol>
   *   <li>Exeucte client getState for Key=key1</li>
   *   <li>Block for result to the the state</li>
   *   <li>Assert the Returned State is the expected to key1</li>
   *   <li>Execute client getState for Key=key2</li>
   *   <li>Execute client deleteState for Key=key2</li>
   *   <li>Block for deleteState call.</li>
   *   <li>Block for getState for Key=key2 and Assert they 2 was not found.</li>
   * </ol>
   *
   * @throws IOException - Test will fail if any unexpected exception is being thrown
   */
  @Test()
  public void testCallbackCalledAtTheExpectedTimeTest() throws IOException {
    String deletedStateKey = "deletedKey";
    String existingState = "existingState";
    String urlDeleteState = STATE_PATH + "/" + deletedStateKey;
    String urlExistingState = STATE_PATH + "/" + existingState;
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3500/" + urlDeleteState)
      .respond(200, ResponseBody.create(MediaType.parse("application/json"),
        deletedStateKey));
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3500/" + urlDeleteState)
      .respond(204);
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3500/" + urlExistingState)
      .respond(200, ResponseBody.create(MediaType.parse("application/json"),
        serializer.serialize(existingState)));
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
    Mono<DaprHttp.Response> response = daprHttp.invokeApi("GET", urlExistingState.split("/"), null, null, Context.empty());
    assertEquals(existingState, serializer.deserialize(response.block().getBody(), String.class));
    Mono<DaprHttp.Response> responseDeleted = daprHttp.invokeApi("GET", urlDeleteState.split("/"), null, null, Context.empty());
    Mono<DaprHttp.Response> responseDeleteKey =
        daprHttp.invokeApi("DELETE", urlDeleteState.split("/"), null, null, Context.empty());
    assertNull(serializer.deserialize(responseDeleteKey.block().getBody(), String.class));
    mockInterceptor.reset();
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3500/" + urlDeleteState)
      .respond(404, ResponseBody.create(MediaType.parse("application/json"),
        "{\"errorCode\":\"404\",\"message\":\"State Not Fuund\"}"));
    try {
      responseDeleted.block();
      fail("Expected DaprException");
    } catch (Exception ex) {
      assertEquals(DaprException.class, ex.getClass());
    }
  }

}
