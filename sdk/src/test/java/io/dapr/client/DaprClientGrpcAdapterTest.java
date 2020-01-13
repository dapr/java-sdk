package io.dapr.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.ObjectSerializer;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

import java.io.IOException;
import java.time.Duration;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprClientGrpcAdapterTest {

  private DaprGrpc.DaprFutureStub client;
  private DaprClientGrpcAdapter adater;
  private ObjectSerializer serializer;

  @Before
  public void setup() {
    client = mock(DaprGrpc.DaprFutureStub.class);
    adater = new DaprClientGrpcAdapter(client);
    serializer = new ObjectSerializer();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unregisterActorTimerTest() {
    Mono<Void> result = adater.unregisterActorTimer("actorType",  "actorId", "timerName");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void registerActorTimerTest() {
    Mono<Void> result = adater.registerActorTimer("actorType",  "actorId", "timerName" , "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unregisterActorReminderTest() {
    Mono<Void> result = adater.unregisterActorReminder("actorType", "actorId", "reminderName");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void registerActorReminderTest() {
    Mono<Void> result = adater.registerActorReminder("actorType", "actorId", "reminderName", "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void saveActorStateTransactionallyTest() {
    Mono<Void> result = adater.saveActorStateTransactionally("actorType", "actorId", "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getActorStateTest() {
    Mono<String> result = adater.getActorState("actorType", "actorId", "keyName");
    String state = result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void invokeActorMethodTest() {
    Mono<String> result = adater.invokeActorMethod("actorType", "actorId", "methodName", "jsonPlayload");
    String monoResult = result.block();
  }

  @Test(expected = RuntimeException.class)
  public void publishEventExceptionThrownTest() {
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adater.publishEvent("topic", "object");
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
    Mono<Void> result = adater.publishEvent("topic", "object");
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
    Mono<Void> result = adater.publishEvent("topic", "object");
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
    Mono<Void> result = adater.publishEvent("topic", event);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeBindingExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adater.invokeBinding("BindingName", "request");
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
    Mono<Void> result = adater.invokeBinding("BindingName", "request");
    result.block();
  }

  @Test
  public void invokeBindingTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adater.invokeBinding("BindingName", "request");
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
    Mono<Void> result = adater.invokeBinding("BindingName", event);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceVoidExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", "request", null);
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
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", "request", null);
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
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", "request", null);
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
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", request, null);
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny("Value")).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
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
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
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
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceObjectTest()  throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", "request", null, String.class);
    String strOutput = result.block();
    assertEquals(serializer.serializeString(resultObj), strOutput);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", null, String.class);
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
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method", null, String.class);
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
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method",null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceNoRequestBodyObjectTest()  throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adater.invokeService(Verb.GET, "appId", "method",null, String.class);
    String strOutput = result.block();
    assertEquals(serializer.serializeString(resultObj), strOutput);
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceByteRequestExceptionThrownTest() throws IOException {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adater.invokeService(Verb.GET, "appId", "method", byteRequest, null);
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
    Mono<byte[]> result = adater.invokeService(Verb.GET, "appId", "method", byteRequest, null);
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
    Mono<byte[]> result = adater.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    byte[] byteOutput = result.block();
    String strOutput = serializer.deserialize(byteOutput, String.class);
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceByteRequestObjectTest()  throws Exception {
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
    Mono<byte[]> result = adater.invokeService(Verb.GET, "appId", "method", byteRequest, null);
    byte[] byteOutput = result.block();
    assertEquals(resultObj, serializer.deserialize(byteOutput, MyObject.class));
  }

  @Test(expected = RuntimeException.class)
  public void invokeServiceNoRequestNoClassBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", null);
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
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", null);
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
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", null);
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(expected)).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyObjectTest()  throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<DaprProtos.InvokeServiceResponseEnvelope> settableFuture = SettableFuture.create();

    MockCallback<DaprProtos.InvokeServiceResponseEnvelope> callback =
        new MockCallback<DaprProtos.InvokeServiceResponseEnvelope>(DaprProtos.InvokeServiceResponseEnvelope.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(DaprProtos.InvokeServiceResponseEnvelope.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adater.invokeService(Verb.GET, "appId", "method", null);
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test(expected = RuntimeException.class)
  public void getStateExceptionThrownTest() {
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class))).thenThrow(RuntimeException.class);
    StateKeyValue<String> key = buildStateKey(null, "Key1", "ETag1");
    Mono<StateKeyValue<String>> result = adater.getState(key, null, String.class);
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
    StateKeyValue<String> key = buildStateKey(null, "Key1", "ETag1");
    Mono<StateKeyValue<String>> result = adater.getState(key, null, String.class);
    settableFuture.setException(ex);
    result.block();
  }

  @Test
  public void getStateStringValueNoOptionsTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    String expectedValue = "Expected state";
    StateKeyValue<String> expectedState = buildStateKey(expectedValue, key, etag);
    DaprProtos.GetStateResponseEnvelope responseEnvelope = DaprProtos.GetStateResponseEnvelope.newBuilder()
        .setData(getAny(expectedValue))
        .setEtag(etag)
        .build();
    SettableFuture<DaprProtos.GetStateResponseEnvelope> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponseEnvelope> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.DaprProtos.GetStateEnvelope.class)))
        .thenReturn(settableFuture);
    StateKeyValue<String> keyRequest = buildStateKey(null, key, etag);
    Mono<StateKeyValue<String>> result = adater.getState(keyRequest, null, String.class);
    settableFuture.set(responseEnvelope);
    assertEquals(expectedState, result.block());
  }

  private <T> StateKeyValue<T> buildStateKey(T value, String key, String etag) {
    return new StateKeyValue(value, key, etag);
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
}
