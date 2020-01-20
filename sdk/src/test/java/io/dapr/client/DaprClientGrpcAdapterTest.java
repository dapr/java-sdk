package io.dapr.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.ObjectSerializer;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprClientGrpcAdapterTest {

  private DaprGrpc.DaprFutureStub client;
  private DaprClientGrpcAdapter adapter;
  private ObjectSerializer serializer;

  @Before
  public void setup() {
    client = mock(DaprGrpc.DaprFutureStub.class);
    adapter = new DaprClientGrpcAdapter(client);
    serializer = new ObjectSerializer();
  }

  @Test(expected = RuntimeException.class)
  public void publishEventExceptionThrownTest() {
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.publishEvent("topic", "object");
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void publishEventCallbackExceptionThrownTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback = new MockCallback<Empty>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.publishEvent("topic", "object");
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void publishEventTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.publishEvent("topic", "object");
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void publishEventObjectTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.publishEvent("topic", event);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeBindingExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeBinding("BindingName", "request");
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeBindingCallbackExceptionThrownTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback =
        new MockCallback<Empty>(ex);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.setException(ex);
    when(client.invokeBinding(any(DaprProtos.InvokeBindingEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeBinding("BindingName", "request");
    result.block();
  }

  @Test
  public void invokeBindingTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeBinding("BindingName", "request");
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingObjectTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingEnvelope.class)))
        .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeBinding("BindingName", event);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceVoidExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceVoidCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(ex);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.setException(ex);
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null);
    result.block();
  }

  @Test
  public void invokeServiceVoidTest() throws Exception {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny("Value")).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null);
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny("Value")).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceVoidObjectTest() throws Exception {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny("Value")).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    MyObject request = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", request, null);
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny("Value")).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void invokeServiceTest() throws Exception {
    String expected = "Value";
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(object)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(object)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<MyObject> result = adapter.invokeService(Verb.GET, "appId", "method", "request", null, MyObject.class);
    MyObject resultObject = result.block();
    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", null, String.class);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", null, String.class);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void invokeServiceNoRequestBodyTest() throws Exception {
    String expected = "Value";
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeService(Verb.GET, "appId", "method", null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceNoRequestBodyObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(object)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(object)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<MyObject> result = adapter.invokeService(Verb.GET, "appId", "method", null, MyObject.class);
    MyObject resultObject = result.block();
    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceByteRequestExceptionThrownTest() throws IOException {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceByteRequestCallbackExceptionThrownTest() throws IOException {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void invokeByteRequestServiceTest() throws Exception {
    String expected = "Value";
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    byte[] byteOutput = result.block();
    String strOutput = serializer.deserialize(byteOutput, String.class);
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceByteRequestObjectTest() throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    byte[] byteOutput = result.block();
    assertEquals(resultObj, serializer.deserialize(byteOutput, MyObject.class));
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestNoClassBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", null);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestNoClassCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", null);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyTest() throws Exception {
    String expected = "Value";
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", null);
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(expected)).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyObjectTest() throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeService(Verb.GET, "appId", "method", null);
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void getStateExceptionThrownTest() {
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class))).thenThrow(RuntimeException.class);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = adapter.getState(key, String.class);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void getStateCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback =
        new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = adapter.getState(key, String.class);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void getStateStringValueNoOptionsTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    String expectedValue = "Expected state";
    State<String> expectedState = buildStateKey(expectedValue, key, etag, null);
    DaprProtos.GetStateResponseEnvelope responseEnvelope = buildGetStateResponseEnvelope(expectedValue, etag);
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> keyRequest = buildStateKey(null, key, etag, null);
    Mono<State<String>> result = adapter.getState(keyRequest, String.class);
    settableFuture.set(responseEnvelope);
    assertEquals(expectedState, result.block());
  }

  @Test
  public void getStateObjectValueWithOptionsTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    MyObject expectedValue = new MyObject(1, "The Value");
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    State<MyObject> expectedState = buildStateKey(expectedValue, key, etag, options);
    DaprProtos.GetStateResponseEnvelope responseEnvelope = DaprProtos.GetStateResponseEnvelope.newBuilder()
        .setData(getAny(expectedValue))
        .setEtag(etag)
        .build();
    State<MyObject> keyRequest = buildStateKey(null, key, etag, options);
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<State<MyObject>> result = adapter.getState(keyRequest, MyObject.class);
    settableFuture.set(responseEnvelope);
    assertEquals(expectedState, result.block());
  }

  @Test
  public void getStateObjectValueWithOptionsNoConcurrencyTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    MyObject expectedValue = new MyObject(1, "The Value");
    StateOptions options = new StateOptions(null, StateOptions.Concurrency.FIRST_WRITE,
        new StateOptions.RetryPolicy(Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR));
    State<MyObject> expectedState = buildStateKey(expectedValue, key, etag, options);
    DaprProtos.GetStateResponseEnvelope responseEnvelope = DaprProtos.GetStateResponseEnvelope.newBuilder()
        .setData(getAny(expectedValue))
        .setEtag(etag)
        .build();
    State<MyObject> keyRequest = buildStateKey(null, key, etag, options);
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<State<MyObject>> result = adapter.getState(keyRequest, MyObject.class);
    settableFuture.set(responseEnvelope);
    assertEquals(expectedState, result.block());
  }

  @Test(expected = RuntimeException.class)
  public void deleteStateExceptionThrowTest() {
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class))).thenThrow(RuntimeException.class);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = adapter.deleteState(key);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void deleteStateCallbackExcpetionThrownTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback =
        new MockCallback<Empty>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = adapter.deleteState(key);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void deleteStateNoOptionsTest() {
    String etag = "ETag1";
    String key = "key1";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, null);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateNoConsistencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateNoConcurrencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateNoRetryPolicyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        null, null, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateRetryPolicyNoDurationTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        null, 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateRetryPolicyNoThresholdTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), null, StateOptions.RetryPolicy.Pattern.LINEAR);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateRetryPolicyNoPatternTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(stateKey);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void saveStateExceptionThrownTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.saveState(key, etag, value, null);
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void saveStateCallbackExceptionThrownTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    Mono<Void> result = adapter.saveState(key, etag, value, null);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void saveStateNoOptionsTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    Mono<Void> result = adapter.saveState(key, etag, value, null);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateNoConsistencyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateNoConcurrencyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null,
        Duration.ofDays(100), 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateNoRetryPolicyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        null, null, null);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateRetryPolicyNoDurationTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        null, 1, StateOptions.RetryPolicy.Pattern.LINEAR);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateRetryPolicyNoThresholdTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), null, StateOptions.RetryPolicy.Pattern.LINEAR);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateRetryPolicyNoPatternTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.DaprProtos.SaveStateEnvelope.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE,
        Duration.ofDays(100), 1, null);
    Mono<Void> result = adapter.saveState(key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  private <T> State<T> buildStateKey(T value, String key, String etag, StateOptions options) {
    return new State(value, key, etag, options);
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
   * @throws Exception
   */

  @Test
  public void getStateDeleteStateThenBlockDeleteThenBlockGet() throws Exception {
    String etag = "ETag1";
    String key1 = "key1";
    String expectedValue1 = "Expected state 1";
    String key2 = "key2";
    String expectedValue2 = "Expected state 2";
    State<String> expectedState1 = buildStateKey(expectedValue1, key1, etag, null);
    Map<String, SettableFuture<DaprProtos.GetStateResponseEnvelope>> futuresMap = new HashMap<>();
    futuresMap.put(key1, buildFutureGetStateEnvelop(expectedValue1, etag));
    futuresMap.put(key2, buildFutureGetStateEnvelop(expectedValue2, etag));
    when(client.getState(argThat(new GetStateEnvelopeKeyMatcher(key1)))).thenReturn(futuresMap.get(key1));
    State<String> keyRequest1 = buildStateKey(null, key1, etag, null);
    Mono<State<String>> resultGet1 = adapter.getState(keyRequest1, String.class);
    assertEquals(expectedState1, resultGet1.block());
    State<String> keyRequest2 = buildStateKey(null, key2, etag, null);
    Mono<State<String>> resultGet2 = adapter.getState(keyRequest2, String.class);

    SettableFuture<Empty> settableFutureDelete = SettableFuture.create();
    MockCallback<Empty> callbackDelete = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFutureDelete, callbackDelete, directExecutor());
    when(client.deleteState(any(io.dapr.DaprProtos.DeleteStateEnvelope.class)))
        .thenReturn(settableFutureDelete);
    Mono<Void> resultDelete = adapter.deleteState(keyRequest2);
    settableFutureDelete.set(Empty.newBuilder().build());
    resultDelete.block();
    assertTrue(callbackDelete.wasCalled);
    futuresMap.replace(key2, null);
    when(client.getState(argThat(new GetStateEnvelopeKeyMatcher(key2)))).thenReturn(futuresMap.get(key2));

    State<String> state2 = resultGet2.block();
    assertNull(state2);
  }

  private <T> SettableFuture<DaprProtos.GetStateResponseEnvelope> buildFutureGetStateEnvelop(T value, String etag) throws IOException {
    DaprProtos.GetStateResponseEnvelope envelope = buildGetStateResponseEnvelope(value, etag);
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback = new MockCallback<>(envelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(envelope);

    return settableFuture;
  }

  private <T> DaprProtos.GetStateResponseEnvelope buildGetStateResponseEnvelope(T value, String etag) throws IOException {
    return DaprProtos.GetStateResponseEnvelope.newBuilder()
        .setData(getAny(value))
        .setEtag(etag)
        .build();
  }

  private StateOptions buildStateOptions(StateOptions.Consistency consistency, StateOptions.Concurrency concurrency,
                                         Duration interval, Integer threshold, StateOptions.RetryPolicy.Pattern pattern) {

    StateOptions.RetryPolicy retryPolicy = null;
    if (interval != null || threshold != null || pattern != null) {
      retryPolicy = new StateOptions.RetryPolicy(interval, threshold, pattern);
    }
    StateOptions options = null;
    if (consistency != null || concurrency != null || retryPolicy != null) {
      options = new StateOptions(consistency, concurrency, retryPolicy);
    }
    return options;
  }

  private <T> Any getAny(T value) throws IOException {
    byte[] byteValue = serializer.serialize(value);
    return Any.newBuilder().setValue(ByteString.copyFrom(byteValue)).build();
  }

  private final class MockCallback<T> implements FutureCallback<T> {
    @Nullable
    private T value = null;
    @Nullable
    private Throwable failure = null;
    private boolean wasCalled = false;

    public MockCallback(T expectedValue) {
      this.value = expectedValue;
    }

    public MockCallback(Throwable expectedFailure) {
      this.failure = expectedFailure;
    }

    @Override
    public synchronized void onSuccess(@NullableDecl T result) {
      assertFalse(wasCalled);
      wasCalled = true;
      assertEquals(value, result);
    }

    @Override
    public synchronized void onFailure(Throwable throwable) {
      assertFalse(wasCalled);
      wasCalled = true;
      assertEquals(failure, throwable);
    }
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

  private static class GetStateEnvelopeKeyMatcher implements ArgumentMatcher<DaprProtos.GetStateEnvelope> {

    private final String propValue;

    GetStateEnvelopeKeyMatcher(String propValue) {
      this.propValue = propValue;
    }

    @Override
    public boolean matches(DaprProtos.GetStateEnvelope argument) {
      if (argument == null) {
        return false;
      }
      if (propValue == null && argument.getKey() != null) {
        return false;
      }
      if (propValue == null && argument.getKey() == null) {
        return true;
      }
      return propValue.equals(argument.getKey());
    }

    @Override
    public String toString() {
      return "<Has property of certain value (propName: " + propValue + ") matcher>";
    }
  }
}
