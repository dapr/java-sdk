/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.DeleteStateRequestBuilder;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequestBuilder;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetBulkStateRequestBuilder;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetStateRequestBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DaprClientGrpcTest {

  private static final String STATE_STORE_NAME = "MyStateStore";

  private static final String SECRET_STORE_NAME = "MySecretStore";

  private Closeable closeable;
  private DaprGrpc.DaprFutureStub client;
  private DaprClientGrpc adapter;
  private ObjectSerializer serializer;

  @Before
  public void setup() throws IOException {
    closeable = mock(Closeable.class);
    client = mock(DaprGrpc.DaprFutureStub.class);
    when(client.withInterceptors(any())).thenReturn(client);
    adapter = new DaprClientGrpc(closeable, client, new DefaultObjectSerializer(), new DefaultObjectSerializer());
    serializer = new ObjectSerializer();
    doNothing().when(closeable).close();
  }

  @After
  public void tearDown() throws Exception {
    adapter.close();
    verify(closeable).close();
    verifyNoMoreInteractions(closeable);
  }

  @Test
  public void waitForSidecarTimeout() throws Exception {
    int port = findFreePort();
    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(port));
    assertThrows(RuntimeException.class, () -> adapter.waitForSidecar(1).block());
  }

  @Test
  public void waitForSidecarTimeoutOK() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      final int port = serverSocket.getLocalPort();
      System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(port));
      Thread t = new Thread(() -> {
        try {
          try (Socket socket = serverSocket.accept()) {
          }
        } catch (IOException e) {
        }
      });
      t.start();
      adapter.waitForSidecar(10000).block();
    }
  }

  @Test
  public void publishEventExceptionThrownTest() {
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenThrow(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));

    assertThrowsDaprException(
        StatusRuntimeException.class,
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: bad bad argument",
        () -> adapter.publishEvent("pubsubname","topic", "object").block());
  }

  @Test
  public void publishEventCallbackExceptionThrownTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument");
    MockCallback<Empty> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.publishEvent("pubsubname","topic", "object");
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: bad bad argument",
        () -> result.block());
  }

  @Test
  public void publishEventSerializeException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    adapter = new DaprClientGrpc(closeable, client, mockSerializer, new DefaultObjectSerializer());
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenReturn(settableFuture);
    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    Mono<Void> result = adapter.publishEvent("pubsubname","topic", "{invalid-json");

    assertThrowsDaprException(
        IOException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void publishEventTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.publishEvent("pubsubname","topic", "object");
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void publishEventNoHotMono() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(Empty.newBuilder().build());
          return settableFuture;
        });
    adapter.publishEvent("pubsubname", "topic", "object");
    // Do not call block() on the mono above, so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void publishEventObjectTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.publishEvent(any(DaprProtos.PublishEventRequest.class)))
        .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.publishEvent("pubsubname", "topic", event);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingIllegalArgumentExceptionTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      // empty binding name
      adapter.invokeBinding("", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null binding name
      adapter.invokeBinding(null, "MyOperation", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null binding operation
      adapter.invokeBinding("BindingName", null, "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty binding operation
      adapter.invokeBinding("BindingName", "", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
  }

  @Test
  public void invokeBindingSerializeException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    adapter = new DaprClientGrpc(closeable, client, mockSerializer, new DefaultObjectSerializer());
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP);

    assertThrowsDaprException(
        IOException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeBindingExceptionThrownTest() {
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeBindingCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.InvokeBindingResponse> callback =
        new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.setException(ex);
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeBindingTest() throws IOException {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
      DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    MockCallback<DaprProtos.InvokeBindingResponse> callback = new MockCallback<>(responseBuilder.build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");
    settableFuture.set(responseBuilder.build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingByteArrayTest() throws IOException {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
        DaprProtos.InvokeBindingResponse.newBuilder().setData(ByteString.copyFrom("OK".getBytes()));
    MockCallback<DaprProtos.InvokeBindingResponse> callback = new MockCallback<>(responseBuilder.build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    Mono<byte[]> result = adapter.invokeBinding("BindingName", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP);
    settableFuture.set(responseBuilder.build());
    assertEquals("OK", new String(result.block(), StandardCharsets.UTF_8));
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingObjectTest() throws IOException {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
      DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    MockCallback<DaprProtos.InvokeBindingResponse> callback = new MockCallback<>(responseBuilder.build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
      .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", event);
    settableFuture.set(responseBuilder.build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingResponseObjectTest() throws IOException {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
        DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    MockCallback<DaprProtos.InvokeBindingResponse> callback = new MockCallback<>(responseBuilder.build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<String> result = adapter.invokeBinding("BindingName", "MyOperation", event, String.class);
    settableFuture.set(responseBuilder.build());
    assertEquals("OK", result.block());
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingResponseObjectTypeRefTest() throws IOException {
    SettableFuture<DaprProtos.InvokeBindingResponse> settableFuture = SettableFuture.create();
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
        DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    MockCallback<DaprProtos.InvokeBindingResponse> callback = new MockCallback<>(responseBuilder.build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenReturn(settableFuture);
    MyObject event = new MyObject(1, "Event");
    Mono<String> result = adapter.invokeBinding("BindingName", "MyOperation", event, TypeRef.get(String.class));
    settableFuture.set(responseBuilder.build());
    assertEquals("OK", result.block());
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeBindingObjectNoHotMono() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeBinding(any(DaprProtos.InvokeBindingRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(Empty.newBuilder().build());
          return settableFuture;
        });
    MyObject event = new MyObject(1, "Event");
    adapter.invokeBinding("BindingName", "MyOperation", event);
    // Do not call block() on mono above, so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void invokeServiceVoidExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceIllegalArgumentExceptionThrownTest() {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    // HttpExtension cannot be null
    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", null);

    assertThrows(IllegalArgumentException.class, () -> result.block());
  }

  @Test
  public void invokeServiceEmptyRequestVoidExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceVoidCallbackExceptionThrownTest() {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.setException(ex);
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceVoidTest() throws Exception {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();

    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny("Value")).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny("Value")).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceVoidObjectTest() throws Exception {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();

    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny("Value")).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    MyObject request = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeMethod("appId", "method", request, HttpExtension.NONE);
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny("Value")).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestClassExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestTypeRefExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null, TypeRef.STRING);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceCallbackExceptionThrownTest() {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceWithHttpExtensionTest() throws IOException {
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, new HashMap<String, String>() {{
      put("test", "1");
    }});
    CommonProtos.InvokeRequest message = CommonProtos.InvokeRequest.newBuilder()
        .setMethod("method")
        .setData(getAny("request"))
        .setContentType("application/json")
        .setHttpExtension(CommonProtos.HTTPExtension.newBuilder()
            .setVerb(CommonProtos.HTTPExtension.Verb.GET)
            .putQuerystring("test", "1").build())
        .build();
    DaprProtos.InvokeServiceRequest request = DaprProtos.InvokeServiceRequest.newBuilder()
        .setId("appId")
        .setMessage(message)
        .build();
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(eq(request)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeMethod("appId", "method", "request", httpExtension, null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceTest() throws Exception {
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(object)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(object)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<MyObject> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, MyObject.class);
    MyObject resultObject = result.block();
    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test
  public void invokeServiceNoRequestBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestCallbackExceptionThrownTest() {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestBodyTest() throws Exception {
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();

    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceNoRequestBodyObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();

    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(object)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(object)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<MyObject> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, MyObject.class);
    MyObject resultObject = result.block();
    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test
  public void invokeServiceByteRequestExceptionThrownTest() throws IOException {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeMethod("appId", "method", byteRequest, HttpExtension.NONE, byte[].class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceByteRequestCallbackExceptionThrownTest() throws IOException {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result =
        adapter.invokeMethod("appId", "method", byteRequest, HttpExtension.NONE,(HashMap<String, String>) null);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeByteRequestServiceTest() throws Exception {
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeMethod(
        "appId", "method", byteRequest, HttpExtension.NONE, (HashMap<String, String>) null);
    byte[] byteOutput = result.block();
    String strOutput = serializer.deserialize(byteOutput, String.class);
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceByteRequestObjectTest() throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeMethod("appId", "method", byteRequest, HttpExtension.NONE, byte[].class);
    byte[] byteOutput = result.block();
    assertEquals(resultObj, serializer.deserialize(byteOutput, MyObject.class));
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyExceptionThrownTest() {
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestNoClassCallbackExceptionThrownTest() {
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyTest() throws Exception {
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void invokeServiceNoRequestNoHotMono() throws Exception {
    String expected = "Value";
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();
    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(expected)).build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
          return settableFuture;
        });
    adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    // Do not call block() on mono above, so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyObjectTest() throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    SettableFuture<CommonProtos.InvokeResponse> settableFuture = SettableFuture.create();

    MockCallback<CommonProtos.InvokeResponse> callback = new MockCallback<>(CommonProtos.InvokeResponse.newBuilder()
            .setData(getAny(resultObj)).build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(CommonProtos.InvokeResponse.newBuilder().setData(getAny(resultObj)).build());
    when(client.invokeService(any(DaprProtos.InvokeServiceRequest.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void getStateIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.getState("", key, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.getState(null, key, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      adapter.getState(STATE_STORE_NAME, (String)null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key
      adapter.getState(STATE_STORE_NAME, "", String.class).block();
    });
  }

  @Test
  public void getStateExceptionThrownTest() {
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class))).thenThrow(RuntimeException.class);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = adapter.getState(STATE_STORE_NAME, key, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void getStateCallbackExceptionThrownTest() {
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<DaprProtos.GetStateResponse> callback =
        new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
        .thenReturn(settableFuture);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = adapter.getState(STATE_STORE_NAME, key, String.class);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void getStateStringValueNoOptionsTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    String expectedValue = "Expected state";
    State<String> expectedState = buildStateKey(expectedValue, key, etag, new HashMap<>(), null);
    DaprProtos.GetStateResponse responseEnvelope = buildGetStateResponse(expectedValue, etag);
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
      .thenReturn(settableFuture);
    State<String> keyRequest = buildStateKey(null, key, etag, null);
    Mono<State<String>> result = adapter.getState(STATE_STORE_NAME, keyRequest, String.class);
    settableFuture.set(responseEnvelope);
    State<String> res = result.block();
    assertNotNull(res);
    assertEquals(expectedState, res);
  }

  @Test
  public void getStateStringValueNoHotMono() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    String expectedValue = "Expected state";
    State<String> expectedState = buildStateKey(expectedValue, key, etag, null);
    DaprProtos.GetStateResponse responseEnvelope = buildGetStateResponse(expectedValue, etag);
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
      .thenAnswer(c -> {
        settableFuture.set(responseEnvelope);
        return settableFuture;
      });
    State<String> keyRequest = buildStateKey(null, key, etag, null);
    adapter.getState(STATE_STORE_NAME, keyRequest, String.class);
    // block() on the mono above is not called, so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void getStateObjectValueWithOptionsTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    MyObject expectedValue = new MyObject(1, "The Value");
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    State<MyObject> expectedState = buildStateKey(expectedValue, key, etag, new HashMap<>(), options);
    DaprProtos.GetStateResponse responseEnvelope = DaprProtos.GetStateResponse.newBuilder()
        .setData(serialize(expectedValue))
        .setEtag(etag)
        .build();
    State<MyObject> keyRequest = buildStateKey(null, key, etag, new HashMap<>(), options);
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
        .thenReturn(settableFuture);
    Mono<State<MyObject>> result = adapter.getState(STATE_STORE_NAME, keyRequest, MyObject.class);
    settableFuture.set(responseEnvelope);
    State<MyObject> res = result.block();
    assertNotNull(res);
    assertEquals(expectedState, res);
  }

  @Test
  public void getStateObjectValueWithMetadataTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    MyObject expectedValue = new MyObject(1, "The Value");
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key_1", "val_1");
    State<MyObject> expectedState = buildStateKey(expectedValue, key, etag, new HashMap<>(), options);
    DaprProtos.GetStateResponse responseEnvelope = DaprProtos.GetStateResponse.newBuilder()
        .setData(serialize(expectedValue))
        .setEtag(etag)
        .build();
    GetStateRequestBuilder builder = new GetStateRequestBuilder(STATE_STORE_NAME, key);
    builder.withMetadata(metadata).withEtag(etag).withStateOptions(options);
    GetStateRequest request = builder.build();
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
        .thenReturn(settableFuture);
    Mono<Response<State<MyObject>>> result = adapter.getState(request, TypeRef.get(MyObject.class));
    settableFuture.set(responseEnvelope);
    Response<State<MyObject>> res = result.block();
    assertNotNull(res);
    assertEquals(expectedState, res.getObject());
  }

  @Test
  public void getStateObjectValueWithOptionsNoConcurrencyTest() throws IOException {
    String etag = "ETag1";
    String key = "key1";
    MyObject expectedValue = new MyObject(1, "The Value");
    StateOptions options = new StateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    State<MyObject> expectedState = buildStateKey(expectedValue, key, etag, new HashMap<>(), options);
    DaprProtos.GetStateResponse responseEnvelope = DaprProtos.GetStateResponse.newBuilder()
        .setData(serialize(expectedValue))
        .setEtag(etag)
        .build();
    State<MyObject> keyRequest = buildStateKey(null, key, etag, new HashMap<>(), options);
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getState(any(io.dapr.v1.DaprProtos.GetStateRequest.class)))
        .thenReturn(settableFuture);
    Mono<State<MyObject>> result = adapter.getState(STATE_STORE_NAME, keyRequest, MyObject.class);
    settableFuture.set(responseEnvelope);
    assertEquals(expectedState, result.block());
  }

  @Test
  public void getStatesIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.getBulkState("", Collections.singletonList("100"), String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.getBulkState(null, Collections.singletonList("100"), String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      // null pointer exception due to keys being converted to an unmodifiable list
      adapter.getBulkState(STATE_STORE_NAME, null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key list
      adapter.getBulkState(STATE_STORE_NAME, Collections.emptyList(), String.class).block();
    });
    // negative parallelism
    GetBulkStateRequest req = new GetBulkStateRequestBuilder(STATE_STORE_NAME, Collections.singletonList("100"))
        .withMetadata(new HashMap<>())
        .withParallelism(-1)
        .build();
    assertThrows(IllegalArgumentException.class, () -> adapter.getBulkState(req, TypeRef.BOOLEAN).block());
  }

  @Test
  public void getStatesString() throws IOException {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    metadata.put("meta2", "value2");
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setData(serialize("hello world"))
            .setKey("100")
            .putAllMetadata(metadata)
            .setEtag("1")
            .build())
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setKey("200")
            .setError("not found")
            .build())
        .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(responseEnvelope);
          return settableFuture;
        });
    List<State<String>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals("hello world", result.stream().findFirst().get().getValue());
    assertEquals(metadata, result.stream().findFirst().get().getMetadata());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesInteger() throws IOException {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setData(serialize(1234))
            .setKey("100")
            .putAllMetadata(metadata)
            .setEtag("1")
            .build())
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setKey("200")
            .setError("not found")
            .build())
        .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(responseEnvelope);
          return settableFuture;
        });
    List<State<Integer>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), int.class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(1234, (int)result.stream().findFirst().get().getValue());
    assertEquals(metadata, result.stream().findFirst().get().getMetadata());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesBoolean() throws IOException {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setData(serialize(true))
            .setKey("100")
            .putAllMetadata(metadata)
            .setEtag("1")
            .build())
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setKey("200")
            .setError("not found")
            .build())
        .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(responseEnvelope);
          return settableFuture;
        });
    List<State<Boolean>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), boolean.class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(true, result.stream().findFirst().get().getValue());
    assertEquals(metadata, result.stream().findFirst().get().getMetadata());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesByteArray() throws IOException {
    Map<String, String> metadata = new HashMap<>();
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setData(serialize(new byte[]{1, 2, 3}))
            .setKey("100")
            .putAllMetadata(metadata)
            .setEtag("1")
            .build())
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setKey("200")
            .setError("not found")
            .build())
        .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(responseEnvelope);
          return settableFuture;
        });
    List<State<byte[]>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), byte[].class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertArrayEquals(new byte[]{1, 2, 3}, result.stream().findFirst().get().getValue());
    assertEquals(0, result.stream().findFirst().get().getMetadata().size());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getStatesObject() throws IOException {
    MyObject object = new MyObject(1, "Event");
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setData(serialize(object))
            .setKey("100")
            .setEtag("1")
            .build())
        .addItems(DaprProtos.BulkStateItem.newBuilder()
            .setKey("200")
            .setError("not found")
            .build())
        .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
        .thenAnswer(c -> {
          settableFuture.set(responseEnvelope);
          return settableFuture;
        });
    List<State<MyObject>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), MyObject.class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals(object, result.stream().findFirst().get().getValue());
    assertEquals(0, result.stream().findFirst().get().getMetadata().size());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void deleteStateExceptionThrowTest() {
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class))).thenThrow(RuntimeException.class);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, key.getKey(), key.getEtag(), key.getOptions());

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void deleteStateIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.deleteState("", key.getKey(), "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.deleteState(null, key.getKey(), "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.deleteState(STATE_STORE_NAME, null, "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.deleteState(STATE_STORE_NAME, "", "etag", null).block();
    });
  }

  @Test
  public void deleteStateCallbackExcpetionThrownTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
        .thenReturn(settableFuture);
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, key.getKey(), key.getEtag(), key.getOptions());
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void deleteStateNoOptionsTest() {
    String etag = "ETag1";
    String key = "key1";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, null);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
      .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
      stateKey.getOptions());
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateWithMetadata() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key_1", "val_1");
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
      .thenReturn(settableFuture);
    DeleteStateRequestBuilder builder = new DeleteStateRequestBuilder(STATE_STORE_NAME, key);
    builder.withEtag(etag).withStateOptions(options).withMetadata(metadata);
    DeleteStateRequest request = builder.build();
    Mono<Response<Void>> result = adapter.deleteState(request);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateTestNoHotMono() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
      .thenAnswer(c -> {
        settableFuture.set(Empty.newBuilder().build());
        return settableFuture;
      });
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
      stateKey.getOptions());
    // Do not call result.block(), so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void deleteStateNoConsistencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void deleteStateNoConcurrencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void executeTransactionIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        key);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.executeStateTransaction("", Collections.singletonList(upsertOperation)).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      adapter.executeStateTransaction(null, Collections.singletonList(upsertOperation)).block();
    });
  }

  @Test
  public void executeTransactionSerializerExceptionTest() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    adapter = new DaprClientGrpc(closeable, client, mockSerializer, mockSerializer);
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    when(client.executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class)))
        .thenReturn(settableFuture);
    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequestBuilder(STATE_STORE_NAME)
        .withTransactionalStates(upsertOperation)
        .build();
    Mono<Response<Void>> result = adapter.executeStateTransaction(request);

    assertThrowsDaprException(
        IOException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void executeTransactionWithMetadataTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("testKey"));
    Map<String, String> metadata = new HashMap<>();
    metadata.put("testKey", "testValue");
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequestBuilder(STATE_STORE_NAME)
        .withTransactionalStates(upsertOperation, deleteOperation)
        .withMetadata(metadata)
        .build();
    Mono<Response<Void>> result = adapter.executeStateTransaction(request);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void executeTransactionTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("testKey")
    );
    Mono<Void> result = adapter.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation, deleteOperation));
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void executeTransactionExceptionThrownTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    when(client.executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class)))
        .thenThrow(RuntimeException.class);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    Mono<Void> result = adapter.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void executeTransactionCallbackExceptionTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("ex");
    MockCallback<Empty> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class)))
        .thenReturn(settableFuture);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    Mono<Void> result = adapter.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: ex",
        () -> result.block());
  }

  @Test
  public void saveStatesIllegalArgumentExceptionTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.saveBulkState("", Collections.emptyList()).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      adapter.saveBulkState(null, Collections.emptyList()).block();
    });
  }

  @Test
  public void saveStateExceptionThrownTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenThrow(RuntimeException.class);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, null);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void saveStateCallbackExceptionThrownTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    RuntimeException ex = new RuntimeException("An Exception");
    MockCallback<Empty> callback = new MockCallback<>(ex);
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, null);
    settableFuture.setException(ex);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void saveStateNoOptionsTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, null);
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
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  @Test
  public void saveStateTestNoHotMono() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenAnswer(c -> {
      settableFuture.set(Empty.newBuilder().build());
      return settableFuture;
    });
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    // No call to result.block(), so nothing should happen.
    assertFalse(callback.wasCalled);
  }

  @Test
  public void saveStateNoConsistencyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    settableFuture.set(Empty.newBuilder().build());
    result.block();
    assertTrue(callback.wasCalled);
  }

  private <T> State<T> buildStateKey(T value, String key, String etag, StateOptions options) {
    return new State<>(value, key, etag, options);
  }

  private <T> State<T> buildStateKey(T value, String key, String etag, Map<String, String> metadata, StateOptions options) {
    return new State<>(value, key, etag, metadata, options);
  }

  /**
   * The purpose of this test is to show that it doesn't matter when the client is called, the actual coll to DAPR
   * will be done when the output Mono response call the Mono.block method.
   * Like for instance if you call getState, without blocking for the response, and then call delete for the same
   * state you just retrieved but block for the delete response, when later you block for the response of the getState,
   * you will not find the state.
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
   * @throws Exception - Test will fail if any unexpected exception is being thrown.
   */

  @Test
  public void getStateThenDelete() throws Exception {
    String etag = "ETag1";
    String key1 = "key1";
    String expectedValue1 = "Expected state 1";
    String key2 = "key2";
    String expectedValue2 = "Expected state 2";
    State<String> expectedState1 = buildStateKey(expectedValue1, key1, etag, new HashMap<>(), null);
    State<String> expectedState2 = buildStateKey(expectedValue2, key2, etag, new HashMap<>(), null);
    Map<String, SettableFuture<DaprProtos.GetStateResponse>> futuresMap = new HashMap<>();
    futuresMap.put(key1, buildFutureGetStateEnvelop(expectedValue1, etag));
    futuresMap.put(key2, buildFutureGetStateEnvelop(expectedValue2, etag));
    when(client.getState(argThat(new GetStateRequestKeyMatcher(key1)))).thenReturn(futuresMap.get(key1));
    when(client.getState(argThat(new GetStateRequestKeyMatcher(key2)))).thenReturn(futuresMap.get(key2));
    State<String> keyRequest1 = buildStateKey(null, key1, etag, null);
    Mono<State<String>> resultGet1 = adapter.getState(STATE_STORE_NAME, keyRequest1, String.class);
    assertEquals(expectedState1, resultGet1.block());
    State<String> keyRequest2 = buildStateKey(null, key2, etag, null);
    Mono<State<String>> resultGet2 = adapter.getState(STATE_STORE_NAME, keyRequest2, String.class);
    assertEquals(expectedState2, resultGet2.block());

    SettableFuture<Empty> settableFutureDelete = SettableFuture.create();
    MockCallback<Empty> callbackDelete = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFutureDelete, callbackDelete, directExecutor());
    when(client.deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class)))
        .thenReturn(settableFutureDelete);
    Mono<Void> resultDelete = adapter.deleteState(STATE_STORE_NAME, keyRequest2.getKey(), keyRequest2.getEtag(),
        keyRequest2.getOptions());
    settableFutureDelete.set(Empty.newBuilder().build());
    resultDelete.block();
    assertTrue(callbackDelete.wasCalled);
  }

  @Test
  public void getStateNullEtag() throws Exception {
    String etag = null;
    String key1 = "key1";
    String expectedValue1 = "Expected state 1";
    State<String> expectedState1 = buildStateKey(expectedValue1, key1, etag, new HashMap<>(), null);
    Map<String, SettableFuture<DaprProtos.GetStateResponse>> futuresMap = new HashMap<>();
    DaprProtos.GetStateResponse envelope = DaprProtos.GetStateResponse.newBuilder()
            .setData(serialize(expectedValue1))
            .build();
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(envelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(envelope);
    futuresMap.put(key1, settableFuture);
    when(client.getState(argThat(new GetStateRequestKeyMatcher(key1)))).thenReturn(futuresMap.get(key1));
    State<String> keyRequest1 = buildStateKey(null, key1, null, null);
    Mono<State<String>> resultGet1 = adapter.getState(STATE_STORE_NAME, keyRequest1, String.class);
    assertEquals(expectedState1, resultGet1.block());
  }

  @Test
  public void getBulkStateNullEtag() throws Exception {
    DaprProtos.GetBulkStateResponse responseEnvelope = DaprProtos.GetBulkStateResponse.newBuilder()
            .addItems(DaprProtos.BulkStateItem.newBuilder()
                    .setData(serialize("hello world"))
                    .setKey("100")
                    .build())
            .addItems(DaprProtos.BulkStateItem.newBuilder()
                    .setKey("200")
                    .setEtag("")
                    .setError("not found")
                    .build())
            .build();
    SettableFuture<DaprProtos.GetBulkStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetBulkStateResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    when(client.getBulkState(any(DaprProtos.GetBulkStateRequest.class)))
            .thenAnswer(c -> {
              settableFuture.set(responseEnvelope);
              return settableFuture;
            });
    List<State<String>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();
    assertTrue(callback.wasCalled);

    assertEquals(2, result.size());
    assertEquals("100", result.stream().findFirst().get().getKey());
    assertEquals("hello world", result.stream().findFirst().get().getValue());
    assertNull(result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());
    assertEquals("200", result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertNull(result.stream().skip(1).findFirst().get().getEtag());
    assertEquals("not found", result.stream().skip(1).findFirst().get().getError());
  }

  @Test
  public void getSecrets() {
    String expectedKey = "attributeKey";
    String expectedValue = "Expected secret value";
    DaprProtos.GetSecretResponse responseEnvelope = buildGetSecretResponse(expectedKey, expectedValue);
    SettableFuture<DaprProtos.GetSecretResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetSecretResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(responseEnvelope);

    when(client.getSecret(any(io.dapr.v1.DaprProtos.GetSecretRequest.class)))
      .thenAnswer(context -> {
        io.dapr.v1.DaprProtos.GetSecretRequest req = context.getArgument(0);
        assertEquals("key", req.getKey());
        assertEquals(SECRET_STORE_NAME, req.getStoreName());
        assertEquals(0, req.getMetadataCount());
        return settableFuture;
      });

    Map<String, String> result = adapter.getSecret(SECRET_STORE_NAME, "key").block();

    assertEquals(1, result.size());
    assertEquals(expectedValue, result.get(expectedKey));
  }

  @Test
  public void getSecretsEmptyResponse() {
    DaprProtos.GetSecretResponse responseEnvelope = buildGetSecretResponse();
    SettableFuture<DaprProtos.GetSecretResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetSecretResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(responseEnvelope);

    when(client.getSecret(any(io.dapr.v1.DaprProtos.GetSecretRequest.class)))
      .thenAnswer(context -> {
        io.dapr.v1.DaprProtos.GetSecretRequest req = context.getArgument(0);
        assertEquals("key", req.getKey());
        assertEquals(SECRET_STORE_NAME, req.getStoreName());
        assertEquals(0, req.getMetadataCount());
        return settableFuture;
      });

    Map<String, String> result = adapter.getSecret(SECRET_STORE_NAME, "key").block();

    assertTrue(result.isEmpty());
  }

  @Test
  public void getSecretsException() {
    SettableFuture<DaprProtos.GetSecretResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetSecretResponse> callback = new MockCallback<>(new RuntimeException());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.setException(new RuntimeException());

    when(client.getSecret(any(io.dapr.v1.DaprProtos.GetSecretRequest.class)))
      .thenAnswer(context -> {
        io.dapr.v1.DaprProtos.GetSecretRequest req = context.getArgument(0);
        assertEquals("key", req.getKey());
        assertEquals(SECRET_STORE_NAME, req.getStoreName());
        assertEquals(0, req.getMetadataCount());
        return settableFuture;
      });

    assertThrowsDaprException(ExecutionException.class, () -> adapter.getSecret(SECRET_STORE_NAME, "key").block());
  }

  @Test
  public void getSecretsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> {
      // empty secret store name
      adapter.getSecret("", "key").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null secret store name
      adapter.getSecret(null, "key").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key
      adapter.getSecret(SECRET_STORE_NAME, "").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      adapter.getSecret(SECRET_STORE_NAME, null).block();
    });
  }

  @Test
  public void getSecretsWithMetadata() {
    String expectedKey = "attributeKey";
    String expectedValue = "Expected secret value";
    DaprProtos.GetSecretResponse responseEnvelope = buildGetSecretResponse(expectedKey, expectedValue);
    SettableFuture<DaprProtos.GetSecretResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetSecretResponse> callback = new MockCallback<>(responseEnvelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(responseEnvelope);

    when(client.getSecret(any(io.dapr.v1.DaprProtos.GetSecretRequest.class)))
      .thenAnswer(context -> {
        io.dapr.v1.DaprProtos.GetSecretRequest req = context.getArgument(0);
        assertEquals("key", req.getKey());
        assertEquals(SECRET_STORE_NAME, req.getStoreName());
        assertEquals("metavalue", req.getMetadataMap().get("metakey"));
        return settableFuture;
      });

    Map<String, String> result = adapter.getSecret(
      SECRET_STORE_NAME,
      "key",
      Collections.singletonMap("metakey", "metavalue")).block();

    assertEquals(1, result.size());
    assertEquals(expectedValue, result.get(expectedKey));
  }

  /* If this test is failing, it means that a new value was added to StateOptions.Consistency
   * enum, without creating a mapping to one of the proto defined gRPC enums
   */
  @Test
  public void stateOptionsConsistencyValuesHaveValidGrpcEnumMappings() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    settableFuture.set(Empty.newBuilder().build());
    for (StateOptions.Consistency consistency : StateOptions.Consistency.values()) {
      StateOptions options = buildStateOptions(consistency, StateOptions.Concurrency.FIRST_WRITE);
      Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
      result.block();
    }

    assertTrue(callback.wasCalled);
  }

  /* If this test is failing, it means that a new value was added to StateOptions.Concurrency
   * enum, without creating a mapping to one of the proto defined gRPC enums
   */
  @Test
  public void stateOptionsConcurrencyValuesHaveValidGrpcEnumMappings() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    when(client.saveState(any(io.dapr.v1.DaprProtos.SaveStateRequest.class))).thenReturn(settableFuture);
    settableFuture.set(Empty.newBuilder().build());
    for (StateOptions.Concurrency concurrency : StateOptions.Concurrency.values()) {
      StateOptions options = buildStateOptions(StateOptions.Consistency.EVENTUAL, concurrency);
      Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
      result.block();
    }

    assertTrue(callback.wasCalled);
  }

  private <T> SettableFuture<DaprProtos.GetStateResponse> buildFutureGetStateEnvelop(T value, String etag) throws IOException {
    DaprProtos.GetStateResponse envelope = buildGetStateResponse(value, etag);
    SettableFuture<DaprProtos.GetStateResponse> settableFuture = SettableFuture.create();
    MockCallback<DaprProtos.GetStateResponse> callback = new MockCallback<>(envelope);
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(envelope);

    return settableFuture;
  }

  private <T> DaprProtos.GetStateResponse buildGetStateResponse(T value, String etag) throws IOException {
    return DaprProtos.GetStateResponse.newBuilder()
        .setData(serialize(value))
        .setEtag(etag)
        .build();
  }

  private DaprProtos.GetSecretResponse buildGetSecretResponse(String key, String value) {
    return DaprProtos.GetSecretResponse.newBuilder()
        .putAllData(Collections.singletonMap(key, value))
        .build();
  }

  private DaprProtos.GetSecretResponse buildGetSecretResponse() {
    return DaprProtos.GetSecretResponse.newBuilder().build();
  }

  private StateOptions buildStateOptions(StateOptions.Consistency consistency, StateOptions.Concurrency concurrency) {
    StateOptions options = null;
    if (consistency != null || concurrency != null) {
      options = new StateOptions(consistency, concurrency);
    }
    return options;
  }

  private Any getAny(Object value) throws IOException {
    return Any.newBuilder().setValue(serialize(value)).build();
  }

  private ByteString serialize(Object value) throws IOException {
    byte[] byteValue = serializer.serialize(value);
    return ByteString.copyFrom(byteValue);
  }

  private static final class MockCallback<T> implements FutureCallback<T> {
    private T value = null;
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
      assertEquals(failure == null, throwable == null);
      if (failure != null) {
        assertEquals(failure.getClass(), throwable.getClass());
      }
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

  private static class GetStateRequestKeyMatcher implements ArgumentMatcher<DaprProtos.GetStateRequest> {

    private final String propValue;

    GetStateRequestKeyMatcher(String propValue) {
      this.propValue = propValue;
    }

    @Override
    public boolean matches(DaprProtos.GetStateRequest argument) {
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

  private static StatusRuntimeException newStatusRuntimeException(String status, String message) {
    return new StatusRuntimeException(Status.fromCode(Status.Code.valueOf(status)).withDescription(message));
  }
}
