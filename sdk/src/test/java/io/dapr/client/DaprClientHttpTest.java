/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.fasterxml.jackson.core.JsonParseException;
import io.dapr.client.domain.DeleteStateRequestBuilder;
import io.dapr.client.domain.GetStateRequestBuilder;
import io.dapr.client.domain.GetBulkStateRequestBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.config.Properties;
import io.dapr.utils.TypeRef;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MediaTypes;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DaprClientHttpTest {

  private static final String STATE_STORE_NAME = "MyStateStore";

  private static final String SECRET_STORE_NAME = "MySecretStore";

  private final String EXPECTED_RESULT =
      "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

  private DaprClientHttp daprClientHttp;

  private DaprHttp daprHttp;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  @Before
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
  }

  @Test
  public void publishEventInvokation() {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/publish/mypubsubname/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    DaprClientHttp daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.publishEvent("mypubsubname", "A", event, null);
    assertNull(mono.block());
  }

  @Test
  public void publishEvent() {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/publish/mypubsubname/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.publishEvent("mypubsubname","A", event);
    assertNull(mono.block());
  }

  @Test
  public void publishEventIfTopicIsNullOrEmpty() {
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.publishEvent("mypubsubname", null, event).block());
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.publishEvent("mypubsubname", "", event).block());
  }

  @Test
  public void publishEventNoHotMono() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/publish/mypubsubname/A")
        .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.publishEvent("mypubsubname", "", event);
    // Should not throw exception because did not call block() on mono above.
  }

  @Test
  public void invokeServiceVerbNull() {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod(null, "", "", null, null, (Class)null).block());
  }

  @Test
  public void invokeServiceIllegalArgumentException() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/invoke/41/method/badorder")
        .respond("INVALID JSON");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // null HttpMethod
      daprClientHttp.invokeMethod("1", "2", "3", new HttpExtension(null, null), null, (Class)null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // null HttpExtension
      daprClientHttp.invokeMethod("1", "2", "3", null, null, (Class)null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // empty appId
      daprClientHttp.invokeMethod("", "1", null, HttpExtension.GET, null, (Class)null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // null appId, empty method
      daprClientHttp.invokeMethod(null, "", null, HttpExtension.POST, null, (Class)null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // empty method
      daprClientHttp.invokeMethod("1", "", null, HttpExtension.PUT, null, (Class)null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      // null method
      daprClientHttp.invokeMethod("1", null, null, HttpExtension.DELETE, null, (Class)null).block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      // invalid JSON response
      daprClientHttp.invokeMethod("41", "badorder", null, HttpExtension.GET, null, String.class).block();
    });
  }


  @Test
  public void invokeServiceMethodNull() {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.invokeMethod("1", "", null, HttpExtension.POST, null, (Class)null).block());
  }

  @Test
  public void invokeService() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
        .respond("\"hello world\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<String> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, null, String.class);
    assertEquals("hello world", mono.block());
  }

  @Test
  public void invokeServiceNullResponse() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
        .respond(new byte[0]);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<String> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, null, String.class);
    assertNull(mono.block());
  }

  @Test
  public void simpleInvokeService() {
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<byte[]> mono = daprClientHttp.invokeMethod("41", "neworder", null, HttpExtension.GET, byte[].class);
    assertEquals(new String(mono.block()), EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithMetadataMap() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<byte[]> mono = daprClientHttp.invokeMethod("41", "neworder", (byte[]) null, HttpExtension.GET, map);
    String monoString = new String(mono.block());
    assertEquals(monoString, EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithOutRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", HttpExtension.GET, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", "", HttpExtension.GET, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequestAndQueryString() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder?test=1")
        .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Map<String, String> queryString = new HashMap<>();
    queryString.put("test", "1");
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, queryString);
    Mono<Void> mono = daprClientHttp.invokeMethod("41", "neworder", "", httpExtension, map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceNoHotMono() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/invoke/41/method/neworder")
        .respond(500);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.invokeMethod("41", "neworder", "", HttpExtension.GET, map);
    // No exception should be thrown because did not call block() on mono above.
  }

  @Test
  public void invokeBinding() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
        .respond("");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "");
    assertNull(mono.block());
  }

  @Test
  public void invokeBindingNullData() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
        .respond("");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", null);
    assertNull(mono.block());
  }

  @Test
  public void invokeBindingErrors() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
        .respond("NOT VALID JSON");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.invokeBinding(null, "myoperation", "").block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.invokeBinding("", "myoperation", "").block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.invokeBinding("topic", null, "").block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.invokeBinding("topic", "", "").block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      daprClientHttp.invokeBinding("sample-topic", "op", "data", String.class).block();
    });
  }

  @Test
  public void invokeBindingResponseNull() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
        .respond(new byte[0]);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<String> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, String.class);
    assertNull(mono.block());
  }

  @Test
  public void invokeBindingResponseObject() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("\"OK\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<String> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, String.class);
    assertEquals("OK", mono.block());
  }

  @Test
  public void invokeBindingResponseDouble() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("1.5");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Double> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", map, double.class);
    assertEquals(1.5, mono.block(), 0.0001);
  }

  @Test
  public void invokeBindingResponseFloat() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("1.5");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Float> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, float.class);
    assertEquals(1.5, mono.block(), 0.0001);
  }

  @Test
  public void invokeBindingResponseChar() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("\"a\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Character> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, char.class);
    assertEquals('a', (char)mono.block());
  }

  @Test
  public void invokeBindingResponseByte() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("\"2\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Byte> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, byte.class);
    assertEquals((byte)0x2, (byte)mono.block());
  }

  @Test
  public void invokeBindingResponseLong() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("1");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Long> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, long.class);
    assertEquals(1, (long)mono.block());
  }

  @Test
  public void invokeBindingResponseInt() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond("1");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Integer> mono = daprClientHttp.invokeBinding("sample-topic", "myoperation", "", null, int.class);
    assertEquals(1, (int)mono.block());
  }

  @Test
  public void invokeBindingNullName() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.invokeBinding(null, "myoperation", "").block());
  }

  @Test
  public void invokeBindingNullOpName() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.invokeBinding("sample-topic", null, "").block());
  }

  @Test
  public void bindingNoHotMono() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/bindings/sample-topic")
        .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.invokeBinding(null, "", "");
    // No exception is thrown because did not call block() on mono above.
  }

  @Test
  public void getStatesErrors() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("NOT VALID JSON");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getBulkState(STATE_STORE_NAME, null, String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getBulkState(STATE_STORE_NAME, new ArrayList<>(), String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getBulkState(null, Arrays.asList("100", "200"), String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getBulkState("", Arrays.asList("100", "200"), String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getBulkState(
          new GetBulkStateRequestBuilder(STATE_STORE_NAME, "100").withParallelism(-1).build(),
          TypeRef.get(String.class)).block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      daprClientHttp.getBulkState(
          new GetBulkStateRequestBuilder(STATE_STORE_NAME, "100").build(),
          TypeRef.get(String.class)).block();
    });
  }

  @Test
  public void getStatesString() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("[{\"key\": \"100\", \"data\": \"hello world\", \"etag\": \"1\"}," +
            "{\"key\": \"200\", \"error\": \"not found\"}]");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    List<State<String>> result =
        daprClientHttp.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();
    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals("hello world", result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesInteger() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("[{\"key\": \"100\", \"data\": 1234, \"etag\": \"1\"}," +
            "{\"key\": \"200\", \"error\": \"not found\"}]");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    List<State<Integer>> result =
        daprClientHttp.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), int.class).block();
    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(1234, (int)result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void getStatesBoolean() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("[{\"key\": \"100\", \"data\": true, \"etag\": \"1\"}," +
            "{\"key\": \"200\", \"error\": \"not found\"}]");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    List<State<Boolean>> result =
        daprClientHttp.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), boolean.class).block();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertTrue((boolean) result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesByteArray() {
    byte[] value = new byte[]{1, 2, 3};
    String base64Value = Base64.getEncoder().encodeToString(value);
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("[{\"key\": \"100\", \"data\": \"" + base64Value + "\", \"etag\": \"1\"}," +
            "{\"key\": \"200\", \"error\": \"not found\"}]");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    // JSON cannot differentiate if data returned is String or byte[], it is ambiguous. So we get base64 encoded back.
    // So, users should use String instead of byte[].
    List<State<String>> result =
        daprClientHttp.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();
    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(base64Value, result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesObject() {
    MyObject object = new MyObject(1, "Event");
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/bulk")
        .respond("[{\"key\": \"100\", \"data\": " +
            "{ \"id\": \"" + object.id + "\", \"value\": \"" + object.value + "\"}, \"etag\": \"1\"}," +
            "{\"key\": \"200\", \"error\": \"not found\"}]");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    // JSON cannot differentiate if data returned is String or byte[], it is ambiguous. So we get base64 encoded back.
    // So, users should use String instead of byte[].
    List<State<MyObject>> result =
        daprClientHttp.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), MyObject.class).block();
    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(object, result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getState() {
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State<>("value", "key", "etag", stateOptions);
    State<String> stateKeyNull = new State<>("value", null, "etag", stateOptions);
    State<String> stateKeyEmpty = new State<>("value", "", "etag", stateOptions);
    State<String> stateKeyBadPayload = new State<>("value", "keyBadPayload", "etag", stateOptions);
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
        .respond("\"" + EXPECTED_RESULT + "\"");
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/keyBadPayload")
        .respond("NOT VALID");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getState(STATE_STORE_NAME, stateKeyNull, String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getState(STATE_STORE_NAME, stateKeyEmpty, String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getState(null, stateKeyValue, String.class).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getState("", stateKeyValue, String.class).block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      daprClientHttp.getState(STATE_STORE_NAME, stateKeyBadPayload, String.class).block();
    });
    Mono<State<String>> mono = daprClientHttp.getState(STATE_STORE_NAME, stateKeyValue, String.class);
    State<String> result = mono.block();
    assertNotNull(result);
    assertEquals(result.getKey(), "key");
  }

  @Test
  public void getStatesEmptyEtag() {
    State<String> stateEmptyEtag = new State<>("value", "key", "", null);
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond("\"" + EXPECTED_RESULT + "\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    State<String> monoEmptyEtag = daprClientHttp.getState(STATE_STORE_NAME, stateEmptyEtag, String.class).block();
    assertEquals(monoEmptyEtag.getKey(), "key");
    assertNull(monoEmptyEtag.getEtag());
  }

  @Test
  public void getStateWithMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key_1", "val_1");
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/key?key_1=val_1")
      .respond("\"" + EXPECTED_RESULT + "\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    GetStateRequestBuilder builder = new GetStateRequestBuilder(STATE_STORE_NAME, "key");
    builder.withMetadata(metadata).withEtag("");
    Mono<Response<State<String>>> monoMetadata = daprClientHttp.getState(builder.build(), TypeRef.get(String.class));
    assertEquals(monoMetadata.block().getObject().getKey(), "key");
  }

  @Test
  public void getStatesNullEtag() {
    State<String> stateNullEtag = new State<>("value", "key", null, null);
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond("\"" + EXPECTED_RESULT + "\"");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    State<String> monoNullEtag = daprClientHttp.getState(STATE_STORE_NAME, stateNullEtag, String.class).block();
    assertEquals(monoNullEtag.getKey(), "key");
    assertNull(monoNullEtag.getEtag());
  }

  @Test
  public void getStatesNoHotMono() {
    State<String> stateNullEtag = new State<>("value", "key", null, null);
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
        .respond(500);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.getState(STATE_STORE_NAME, stateNullEtag, String.class);
    // No exception should be thrown since did not call block() on mono above.
  }

  @Test
  public void saveStates() {
    State<String> stateKeyValue = new State<>("value", "key", "etag", null);
    List<State<?>> stateKeyValueList = Collections.singletonList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.saveBulkState(STATE_STORE_NAME, stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void saveStatesErrors() {
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.saveBulkState(null, null).block());
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.saveBulkState("", null).block());
  }

  @Test
  public void saveStatesNull() {
    List<State<?>> stateKeyValueList = new ArrayList<>();
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.saveBulkState(STATE_STORE_NAME, null);
    assertNull(mono.block());
    Mono<Void> mono1 = daprClientHttp.saveBulkState(STATE_STORE_NAME, stateKeyValueList);
    assertNull(mono1.block());
  }

  @Test
  public void saveStatesNullState() {
    List<State<?>> stateKeyValueList = new ArrayList<>();
    stateKeyValueList.add(null);
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
        .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono1 = daprClientHttp.saveBulkState(STATE_STORE_NAME, stateKeyValueList);
    assertNull(mono1.block());
  }

  @Test
  public void saveStatesEtagNull() {
    State<String> stateKeyValue = new State<>("value", "key", null, null);
    List<State<?>> stateKeyValueList = Collections.singletonList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.saveBulkState(STATE_STORE_NAME, stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void saveStatesEtagEmpty() {
    State<String> stateKeyValue = new State<>("value", "key", "", null);
    List<State<?>> stateKeyValueList = Collections.singletonList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.saveBulkState(STATE_STORE_NAME, stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void simpleSaveStates() {
    mockInterceptor.addRule()
      .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
      .respond(EXPECTED_RESULT);
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.saveState(STATE_STORE_NAME, "key", "etag", "value", stateOptions);
    assertNull(mono.block());
  }

  @Test
  public void saveStatesNoHotMono() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore")
        .respond(500);
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.saveState(STATE_STORE_NAME, "key", "etag", "value", stateOptions);
    // No exception should be thrown because we did not call block() on the mono above.
  }

  @Test
  public void simpleExecuteTransaction() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/transaction")
        .respond(EXPECTED_RESULT);
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);

    State<String> stateKey = new State<>(data, key, etag, stateOptions);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("deleteKey"));
    Mono<Void> mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation,
        deleteOperation));
    assertNull(mono.block());
  }

  @Test
  public void simpleExecuteTransactionNullEtag() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/transaction")
        .respond(EXPECTED_RESULT);
    String etag = null;
    String key = "key1";
    String data = "my data";
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);

    State<String> stateKey = new State<>(data, key, etag, stateOptions);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("deleteKey"));
    Mono<Void> mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation,
        deleteOperation));
    assertNull(mono.block());
  }

  @Test
  public void simpleExecuteTransactionEmptyEtag() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/transaction")
        .respond(EXPECTED_RESULT);
    String etag = "empty";
    String key = "key1";
    String data = "my data";
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);

    State<String> stateKey = new State<>(data, key, etag, stateOptions);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("deleteKey"));
    Mono<Void> mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation,
        deleteOperation));
    assertNull(mono.block());
  }

  @Test
  public void simpleExecuteTransactionNullOperationAndNullState() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/transaction")
        .respond(EXPECTED_RESULT);
    String etag = null;
    String key = "key1";
    String data = "my data";
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);

    State<String> stateKey = new State<>(data, key, etag, stateOptions);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("deleteKey"));
    TransactionalStateOperation<String> nullStateOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        null);
    Mono<Void> mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(
        null,
        nullStateOperation,
        upsertOperation,
        deleteOperation));
    assertNull(mono.block());
  }

  @Test
  public void executeTransactionErrors() {
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.executeStateTransaction(null,  null).block());
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.executeStateTransaction("",  null).block());
  }

  @Test
  public void simpleExecuteTransactionNull() {
    mockInterceptor.addRule()
        .post("http://127.0.0.1:3000/v1.0/state/MyStateStore/transaction")
        .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME,  null);
    assertNull(mono.block());
    mono = daprClientHttp.executeStateTransaction(STATE_STORE_NAME,  Collections.emptyList());
    assertNull(mono.block());
  }

  @Test
  public void deleteState() {
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State<>("value", "key", "etag", stateOptions);
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.deleteState(STATE_STORE_NAME, stateKeyValue.getKey(), stateKeyValue.getEtag(), stateOptions);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateWithMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key_1", "val_1");
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State<>("value", "key", "etag", stateOptions);
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key?key_1=val_1")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    DeleteStateRequestBuilder builder = new DeleteStateRequestBuilder(STATE_STORE_NAME, stateKeyValue.getKey());
    builder.withMetadata(metadata).withEtag(stateKeyValue.getEtag()).withStateOptions(stateOptions);
    Mono<Response<Void>> monoMetadata = daprClientHttp.deleteState(builder.build());
    assertNull(monoMetadata.block().getObject());
  }

  @Test
  public void deleteStateNoHotMono() {
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State<>("value", "key", "etag", stateOptions);
    mockInterceptor.addRule()
        .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
        .respond(500);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.deleteState(STATE_STORE_NAME, stateKeyValue.getKey(), stateKeyValue.getEtag(), stateOptions);
    // No exception should be thrown because we did not call block() on the mono above.
  }

  @Test
  public void deleteStateNullEtag() {
    State<String> stateKeyValue = new State<>("value", "key", null, null);
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.deleteState(STATE_STORE_NAME, stateKeyValue.getKey(), stateKeyValue.getEtag(), null);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateEmptyEtag() {
    State<String> stateKeyValue = new State<>("value", "key", "", null);
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    Mono<Void> mono = daprClientHttp.deleteState(STATE_STORE_NAME, stateKeyValue.getKey(), stateKeyValue.getEtag(), null);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateIllegalArgumentException() {
    State<String> stateKeyValueNull = new State<>("value", null, "etag", null);
    State<String> stateKeyValueEmpty = new State<>("value", "", "etag", null);
    mockInterceptor.addRule()
      .delete("http://127.0.0.1:3000/v1.0/state/MyStateStore/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState(STATE_STORE_NAME, null, null, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState(STATE_STORE_NAME, "", null, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState(STATE_STORE_NAME, " ", null, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState(null, "key", null, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState("", "key", null, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.deleteState(" ", "key", null, null).block();
    });
  }

  @Test
  public void getSecrets() {
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
      .respond("{ \"mysecretkey\": \"mysecretvalue\"}");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getSecret(SECRET_STORE_NAME, null).block();
    });
    Map<String, String> secret = daprClientHttp.getSecret(SECRET_STORE_NAME, "key").block();

    assertEquals(1, secret.size());
    assertEquals("mysecretvalue", secret.get("mysecretkey"));
  }

  @Test
  public void getSecretsEmpty() {
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
      .respond("");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getSecret(SECRET_STORE_NAME, null).block();
    });
    Map<String, String> secret = daprClientHttp.getSecret(SECRET_STORE_NAME, "key").block();

    assertTrue(secret.isEmpty());
  }

  @Test
  public void getSecrets404() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
        .respond(404);
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException("UNKNOWN", () ->
        daprClientHttp.getSecret(SECRET_STORE_NAME, "key").block()
    );
  }

  @Test
  public void getSecrets404WithErrorCode() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
        .respond(404,
            ResponseBody.create("" +
                "{\"errorCode\":\"ERR_SECRET_STORE_NOT_FOUND\"," +
                "\"message\":\"error message\"}", MediaTypes.MEDIATYPE_JSON));
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException("ERR_SECRET_STORE_NOT_FOUND", "ERR_SECRET_STORE_NOT_FOUND: error message", () ->
        daprClientHttp.getSecret(SECRET_STORE_NAME, "key").block()
    );
  }

  @Test
  public void getSecretsErrors() {
    mockInterceptor.addRule()
        .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
        .respond("INVALID JSON");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.getSecret(null, "key").block());
    assertThrowsDaprException(IllegalArgumentException.class, () ->
        daprClientHttp.getSecret("", "key").block());
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getSecret(SECRET_STORE_NAME, null).block();
    });
    assertThrowsDaprException(IllegalArgumentException.class, () -> {
      daprClientHttp.getSecret(SECRET_STORE_NAME, "").block();
    });
    assertThrowsDaprException(JsonParseException.class, () -> {
      daprClientHttp.getSecret(SECRET_STORE_NAME, "key").block();
    });
  }

  @Test
  public void getSecretsWithMetadata() {
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key")
      .respond("{ \"mysecretkey\": \"mysecretvalue\"}");
    mockInterceptor.addRule()
      .get("http://127.0.0.1:3000/v1.0/secrets/MySecretStore/key?metakey=metavalue")
      .respond("{ \"mysecretkey2\": \"mysecretvalue2\"}");
    daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3000, okHttpClient);
    daprClientHttp = new DaprClientHttp(daprHttp);
    {
      Map<String, String> secret = daprClientHttp.getSecret(
        SECRET_STORE_NAME,
        "key",
        null).block();

      assertEquals(1, secret.size());
      assertEquals("mysecretvalue", secret.get("mysecretkey"));
    }

    {
      Map<String, String> secret = daprClientHttp.getSecret(
        SECRET_STORE_NAME,
        "key",
        Collections.singletonMap("metakey", "metavalue")).block();

      assertEquals(1, secret.size());
      assertEquals("mysecretvalue2", secret.get("mysecretkey2"));
    }
  }

  @Test
  public void closeException() {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doThrow(new IllegalStateException()).when(daprHttp).close();

    // This method does not throw DaprException because IOException is expected by the Closeable interface.
    daprClientHttp = new DaprClientHttp(daprHttp);
    assertThrowsDaprException(IllegalStateException.class, () -> daprClientHttp.close());
  }

  @Test
  public void close() {
    DaprHttp daprHttp = Mockito.mock(DaprHttp.class);
    Mockito.doNothing().when(daprHttp).close();

    // This method does not throw DaprException because IOException is expected by the Closeable interface.
    daprClientHttp = new DaprClientHttp(daprHttp);
    daprClientHttp.close();
  }

  public static class MyObject {
    private Integer id;
    private String value;

    public MyObject() {
    }

    public MyObject(Integer id, String value) {
      this.id = id;
      this.value = value;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MyObject)) return false;

      MyObject myObject = (MyObject) o;

      if (!getId().equals(myObject.getId())) return false;
      if (getValue() != null ? !getValue().equals(myObject.getValue()) : myObject.getValue() != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = getId().hashCode();
      result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
      return result;
    }
  }
}