/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.ObjectSerializer;
import okhttp3.OkHttpClient;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DaprClientHttpAdapterTest {

  private DaprClientHttpAdapter daprClientHttpAdapter;

  private DaprHttp daprHttp;

  private OkHttpClient okHttpClient;

  private MockInterceptor mockInterceptor;

  private ObjectSerializer serializer = new ObjectSerializer();

  private final String EXPECTED_RESULT = "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";


  @Before
  public void setUp() throws Exception {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
  }

  @Test
  public void publishEventInvokation() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    DaprClientHttpAdapter daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.publishEvent("A", event, null);
    assertNull(mono.block());
  }

  @Test
  public void publishEvent() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.publishEvent("A", event);
    assertNull(mono.block());
  }

  @Test(expected = IllegalArgumentException.class)
  public void publishEventIfTopicIsNull() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.publishEvent("", event);
    assertNull(mono.block());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invokeServiceVerbNull() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeService(null, "", "", null, null, null);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceIllegalArgumentException() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.invokeService(null, "", "", null, null, null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.invokeService(Verb.POST, null, "", null, null, null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.invokeService(Verb.POST, "", "", null, null, null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.invokeService(Verb.POST, "1", null, null, null, null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.invokeService(Verb.POST, "1", "", null, null, null).block();
    });
  }


  @Test(expected = IllegalArgumentException.class)
  public void invokeServiceMethodNull() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/publish/A")
      .respond(EXPECTED_RESULT);
    String event = "{ \"message\": \"This is a test\" }";
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeService(Verb.POST, "1", "", null, null, null);
    assertNull(mono.block());
  }

  @Test
  public void invokeService() {
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<String> mono = daprClientHttpAdapter.invokeService(Verb.GET, "41", "neworder", null, null, String.class);
    assertEquals(mono.block(), EXPECTED_RESULT);
  }

  @Test
  public void simpleInvokeService() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<String> mono = daprClientHttpAdapter.invokeService(Verb.GET, "41", "neworder", null, String.class);
    assertEquals(mono.block(), EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithMaps() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<byte[]> mono = daprClientHttpAdapter.invokeService(Verb.GET, "41", "neworder", (byte[]) null, map);
    String monoString = new String(mono.block());
    assertEquals(monoString, EXPECTED_RESULT);
  }

  @Test
  public void invokeServiceWithOutRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeService(Verb.GET, "41", "neworder", map);
    assertNull(mono.block());
  }

  @Test
  public void invokeServiceWithRequest() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/invoke/41/method/neworder")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeService(Verb.GET, "41", "neworder", "", map);
    assertNull(mono.block());
  }

  @Test
  public void invokeBinding() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/bindings/sample-topic")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeBinding("sample-topic", "");
    assertNull(mono.block());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invokeBindingNullName() {
    Map<String, String> map = new HashMap<>();
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/bindings/sample-topic")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.invokeBinding(null, "");
    assertNull(mono.block());
  }


  @Test
  public void getStates() {
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State("value", "key", "etag", stateOptions);
    State<String> stateKeyNull = new State("value", null, "etag", stateOptions);
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.getState(stateKeyNull, String.class).block();
    });
    Mono<State<String>> mono = daprClientHttpAdapter.getState(stateKeyValue, String.class);
    assertEquals(mono.block().getKey(), "key");
  }

  @Test
  public void getStatesEmptyEtag() {
    State<String> stateEmptyEtag = new State("value", "key", "", null);
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<State<String>> monoEmptyEtag = daprClientHttpAdapter.getState(stateEmptyEtag, String.class);
    assertEquals(monoEmptyEtag.block().getKey(), "key");
  }

  @Test
  public void getStatesNullEtag() {
    State<String> stateNullEtag = new State("value", "key", null, null);
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<State<String>> monoNullEtag = daprClientHttpAdapter.getState(stateNullEtag, String.class);
    assertEquals(monoNullEtag.block().getKey(), "key");
  }

  @Test
  public void saveStates() {
    State<String> stateKeyValue = new State("value", "key", "etag", null);
    List<State<String>> stateKeyValueList = Arrays.asList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/state")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveStates(stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void saveStatesNull() {
    State<String> stateKeyValue = new State("value", "key", "", null);
    List<State<String>> stateKeyValueList = new ArrayList();
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/state")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveStates(null);
    assertNull(mono.block());
    Mono<Void> mono1 = daprClientHttpAdapter.saveStates(stateKeyValueList);
    assertNull(mono1.block());
  }

  @Test
  public void saveStatesEtagNull() {
    State<String> stateKeyValue = new State("value", "key", null, null);
    List<State<String>> stateKeyValueList = Arrays.asList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/state")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveStates(stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void saveStatesEtagEmpty() {
    State<String> stateKeyValue = new State("value", "key", "", null);
    List<State<String>> stateKeyValueList = Arrays.asList(stateKeyValue);
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/state")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveStates(stateKeyValueList);
    assertNull(mono.block());
  }

  @Test
  public void simpleSaveStates() {
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/state")
      .respond(EXPECTED_RESULT);
    StateOptions stateOptions = mock(StateOptions.class);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveState("key", "etag", "value", stateOptions);
    assertNull(mono.block());
  }


  @Test
  public void deleteState() {
    StateOptions stateOptions = mock(StateOptions.class);
    State<String> stateKeyValue = new State("value", "key", "etag", stateOptions);
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.deleteState(stateKeyValue);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateNullEtag() {
    State<String> stateKeyValue = new State("value", "key", null, null);
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.deleteState(stateKeyValue);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateEmptyEtag() {
    State<String> stateKeyValue = new State("value", "key", "", null);
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.deleteState(stateKeyValue);
    assertNull(mono.block());
  }

  @Test
  public void deleteStateIllegalArgumentException() {
    State<String> stateKeyValueNull = new State("value", null, "etag", null);
    State<String> stateKeyValueEmpty = new State("value", "", "etag", null);
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/state/key")
      .respond(EXPECTED_RESULT);
    daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.deleteState(null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.deleteState(stateKeyValueNull).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      daprClientHttpAdapter.deleteState(stateKeyValueEmpty).block();
    });
  }

  @Test
  public void invokeActorMethod() throws IOException {
    DaprHttp daprHttpMock = mock(DaprHttp.class);
    mockInterceptor.addRule()
      .post("http://localhost:3000/v1.0/actors/DemoActor/1/method/Payment")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<String> mono = daprClientHttpAdapter.invokeActorMethod("DemoActor", "1", "Payment", "");
    assertEquals(mono.block(), EXPECTED_RESULT);
  }


  @Test
  public void getActorState() {
    mockInterceptor.addRule()
      .get("http://localhost:3000/v1.0/actors/DemoActor/1/state/order")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<String> mono = daprClientHttpAdapter.getActorState("DemoActor", "1", "order");
    assertEquals(mono.block(), EXPECTED_RESULT);
  }


  @Test
  public void saveActorStateTransactionally() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/state")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.saveActorStateTransactionally("DemoActor", "1", "");
    assertNull(mono.block());
  }

  @Test
  public void registerActorReminder() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.registerActorReminder("DemoActor", "1", "reminder", "");
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorReminder() {
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/actors/DemoActor/1/reminders/reminder")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.unregisterActorReminder("DemoActor", "1", "reminder");
    assertNull(mono.block());
  }

  @Test
  public void registerActorTimer() {
    mockInterceptor.addRule()
      .put("http://localhost:3000/v1.0/actors/DemoActor/1/timers/timer")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.registerActorTimer("DemoActor", "1", "timer", "");
    assertNull(mono.block());
  }

  @Test
  public void unregisterActorTimer() {
    mockInterceptor.addRule()
      .delete("http://localhost:3000/v1.0/actors/DemoActor/1/timers/timer")
      .respond(EXPECTED_RESULT);
    DaprHttp daprHttp = new DaprHttp(3000, okHttpClient);
    daprClientHttpAdapter = new DaprClientHttpAdapter(daprHttp);
    Mono<Void> mono = daprClientHttpAdapter.unregisterActorTimer("DemoActor", "1", "timer");
    assertNull(mono.block());
  }

}