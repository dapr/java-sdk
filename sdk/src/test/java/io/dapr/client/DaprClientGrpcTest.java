/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

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
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Answer;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprClientGrpcTest {

  private static final String STATE_STORE_NAME = "MyStateStore";

  private static final String SECRET_STORE_NAME = "MySecretStore";

  private Closeable closeable;
  private DaprGrpc.DaprStub client;
  private DaprClientGrpc adapter;
  private ObjectSerializer serializer;

  @Before
  public void setup() throws IOException {
    closeable = mock(Closeable.class);
    client = mock(DaprGrpc.DaprStub.class);
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
    doAnswer((Answer<Void>) invocation -> {
      throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument");
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    assertThrowsDaprException(
            StatusRuntimeException.class,
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: bad bad argument",
            () -> adapter.publishEvent("pubsubname","topic", "object").block());
  }

  @Test
  public void publishEventCallbackExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));
      return null;
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    Mono<Void> result = adapter.publishEvent("pubsubname","topic", "object");

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    Mono<Void> result = adapter.publishEvent("pubsubname","topic", "object");
    result.block();
  }

  @Test
  public void publishEventNoHotMono() {
    AtomicBoolean called = new AtomicBoolean(false);
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());
    adapter.publishEvent("pubsubname", "topic", "object");
    // Do not call block() on the mono above, so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void publishEventObjectTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.publishEvent("pubsubname", "topic", event);
    result.block();
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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

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
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeBindingCallbackExceptionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeBindingTest() throws IOException {
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
      DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", "request");
    result.block();
  }

  @Test
  public void invokeBindingByteArrayTest() {
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
        DaprProtos.InvokeBindingResponse.newBuilder().setData(ByteString.copyFrom("OK".getBytes()));
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<byte[]> result = adapter.invokeBinding("BindingName", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP);

    assertEquals("OK", new String(result.block(), StandardCharsets.UTF_8));
  }

  @Test
  public void invokeBindingObjectTest() throws IOException {
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
      DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeBinding("BindingName", "MyOperation", event);

    result.block();
  }

  @Test
  public void invokeBindingResponseObjectTest() throws IOException {
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
        DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<String> result = adapter.invokeBinding("BindingName", "MyOperation", event, String.class);

    assertEquals("OK", result.block());
  }

  @Test
  public void invokeBindingResponseObjectTypeRefTest() throws IOException {
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
            DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<String> result = adapter.invokeBinding("BindingName", "MyOperation", event, TypeRef.get(String.class));

    assertEquals("OK", result.block());
  }

  @Test
  public void invokeBindingObjectNoHotMono() throws IOException {
    AtomicBoolean called = new AtomicBoolean(false);
    DaprProtos.InvokeBindingResponse.Builder responseBuilder =
            DaprProtos.InvokeBindingResponse.newBuilder().setData(serialize("OK"));
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<DaprProtos.InvokeBindingResponse> observer = (StreamObserver<DaprProtos.InvokeBindingResponse>) invocation.getArguments()[1];
      observer.onNext(responseBuilder.build());
      observer.onCompleted();
      return null;
    }).when(client).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());
    MyObject event = new MyObject(1, "Event");
    adapter.invokeBinding("BindingName", "MyOperation", event);
    // Do not call block() on mono above, so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void invokeServiceVoidExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceIllegalArgumentExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny("Value")).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    // HttpExtension cannot be null
    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", null);

    assertThrows(IllegalArgumentException.class, () -> result.block());
  }

  @Test
  public void invokeServiceEmptyRequestVoidExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceVoidCallbackExceptionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceVoidTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny("Value")).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE);
    result.block();
  }

  @Test
  public void invokeServiceVoidObjectTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny("Value")).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    MyObject request = new MyObject(1, "Event");
    Mono<Void> result = adapter.invokeMethod("appId", "method", request, HttpExtension.NONE);
    result.block();
  }

  @Test
  public void invokeServiceExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestClassExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestTypeRefExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", HttpExtension.NONE, (Map<String, String>)null, TypeRef.STRING);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceCallbackExceptionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceWithHttpExtensionTest() throws IOException {
    HttpExtension httpExtension = new HttpExtension(
        DaprHttp.HttpMethods.GET, Collections.singletonMap("test", "1"), null);
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

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(eq(request), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", "request", httpExtension, null, String.class);
    String strOutput = result.block();
    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceTest() {
    String expected = "Value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, String.class);
    String strOutput = result.block();

    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(object)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<MyObject> result = adapter.invokeMethod("appId", "method", "request", HttpExtension.NONE, null, MyObject.class);
    MyObject resultObject = result.block();

    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test
  public void invokeServiceNoRequestBodyExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestCallbackExceptionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestBodyTest() throws Exception {
    String expected = "Value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<String> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, String.class);
    String strOutput = result.block();

    assertEquals(expected, strOutput);
  }

  @Test
  public void invokeServiceNoRequestBodyObjectTest() throws Exception {
    MyObject object = new MyObject(1, "Value");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(object)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<MyObject> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE, MyObject.class);
    MyObject resultObject = result.block();

    assertEquals(object.id, resultObject.id);
    assertEquals(object.value, resultObject.value);
  }

  @Test
  public void invokeServiceByteRequestExceptionThrownTest() throws IOException {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());
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
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());
    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);

    Mono<byte[]> result =
        adapter.invokeMethod("appId", "method", byteRequest, HttpExtension.NONE,(HashMap<String, String>) null);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeByteRequestServiceTest() throws Exception {
    String expected = "Value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());
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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(resultObj)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    String request = "Request";
    byte[] byteRequest = serializer.serialize(request);
    Mono<byte[]> result = adapter.invokeMethod("appId", "method", byteRequest, HttpExtension.NONE, byte[].class);
    byte[] byteOutput = result.block();

    assertEquals(resultObj, serializer.deserialize(byteOutput, MyObject.class));
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());
    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestNoClassCallbackExceptionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);

    assertThrowsDaprException(
        ExecutionException.class,
        "UNKNOWN",
        "UNKNOWN: java.lang.RuntimeException: An Exception",
        () -> result.block());
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyTest() {
    String expected = "Value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    result.block();
  }

  @Test
  public void invokeServiceNoRequestNoHotMono() {
    AtomicBoolean called = new AtomicBoolean(false);
    String expected = "Value";
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(expected)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());
    adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    // Do not call block() on mono above, so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void invokeServiceNoRequestNoClassBodyObjectTest() throws Exception {
    MyObject resultObj = new MyObject(1, "Value");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<CommonProtos.InvokeResponse> observer = (StreamObserver<CommonProtos.InvokeResponse>) invocation.getArguments()[1];
      observer.onNext(CommonProtos.InvokeResponse.newBuilder().setData(getAny(resultObj)).build());
      observer.onCompleted();
      return null;
    }).when(client).invokeService(any(DaprProtos.InvokeServiceRequest.class), any());

    Mono<Void> result = adapter.invokeMethod("appId", "method", (Object)null, HttpExtension.NONE);
    result.block();
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
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

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
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = adapter.getState(STATE_STORE_NAME, key, String.class);

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> keyRequest = buildStateKey(null, key, etag, null);
    Mono<State<String>> result = adapter.getState(STATE_STORE_NAME, keyRequest, String.class);
    State<String> res = result.block();

    assertNotNull(res);
    assertEquals(expectedState, res);
  }

  @Test
  public void getStateStringValueNoHotMono() throws IOException {
    AtomicBoolean called = new AtomicBoolean(false);
    String etag = "ETag1";
    String key = "key1";
    String expectedValue = "Expected state";
    DaprProtos.GetStateResponse responseEnvelope = buildGetStateResponse(expectedValue, etag);
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> keyRequest = buildStateKey(null, key, etag, null);
    adapter.getState(STATE_STORE_NAME, keyRequest, String.class);
    // block() on the mono above is not called, so nothing should happen.
    assertFalse(called.get());
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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<State<MyObject>> result = adapter.getState(STATE_STORE_NAME, keyRequest, MyObject.class);
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
    builder.withMetadata(metadata).withStateOptions(options);
    GetStateRequest request = builder.build();
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<Response<State<MyObject>>> result = adapter.getState(request, TypeRef.get(MyObject.class));
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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<State<MyObject>> result = adapter.getState(STATE_STORE_NAME, keyRequest, MyObject.class);

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<String>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());
    List<State<Integer>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), int.class).block();

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<Boolean>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), boolean.class).block();

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<byte[]>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), byte[].class).block();

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<MyObject>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), MyObject.class).block();

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
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

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
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, key.getKey(), key.getEtag(), key.getOptions());

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, null);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    result.block();
  }

  @Test
  public void deleteStateTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
      stateKey.getOptions());
    result.block();
  }

  @Test
  public void deleteStateWithMetadata() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key_1", "val_1");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    DeleteStateRequestBuilder builder = new DeleteStateRequestBuilder(STATE_STORE_NAME, key);
    builder.withEtag(etag).withStateOptions(options).withMetadata(metadata);
    DeleteStateRequest request = builder.build();
    Mono<Response<Void>> result = adapter.deleteState(request);
    result.block();
  }

  @Test
  public void deleteStateTestNoHotMono() {
    AtomicBoolean called = new AtomicBoolean(false);
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
            stateKey.getOptions());
    // Do not call result.block(), so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void deleteStateNoConsistencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    result.block();
  }

  @Test
  public void deleteStateNoConcurrencyTest() {
    String etag = "ETag1";
    String key = "key1";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = adapter.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
        stateKey.getOptions());
    result.block();
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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());


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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

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
    result.block();
  }

  @Test
  public void executeTransactionTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("testKey")
    );
    Mono<Void> result = adapter.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation, deleteOperation));
    result.block();
  }

  @Test
  public void executeTransactionExceptionThrownTest() {
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

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
    RuntimeException ex = new RuntimeException("ex");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    Mono<Void> result = adapter.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));

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
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

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
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, null);

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());


    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, null);
    result.block();
  }

  @Test
  public void saveStateTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
  }

  @Test
  public void saveStateTestNoHotMono() {
    AtomicBoolean called = new AtomicBoolean(false);
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      called.set(true);
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    // No call to result.block(), so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void saveStateNoConsistencyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
  }

  @Test
  public void saveStateNoConcurrencyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
  }

  @Test
  public void saveStateNoRetryPolicyTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
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
    Map<String, DaprProtos.GetStateResponse> futuresMap = new HashMap<>();
    futuresMap.put(key1, buildFutureGetStateEnvelop(expectedValue1, etag));
    futuresMap.put(key2, buildFutureGetStateEnvelop(expectedValue2, etag));

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(futuresMap.get(key1));
      observer.onCompleted();
      return null;
    }).when(client).getState(argThat(new GetStateRequestKeyMatcher(key1)), any());
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(futuresMap.get(key2));
      observer.onCompleted();
      return null;
    }).when(client).getState(argThat(new GetStateRequestKeyMatcher(key2)), any());

    State<String> keyRequest1 = buildStateKey(null, key1, etag, null);
    Mono<State<String>> resultGet1 = adapter.getState(STATE_STORE_NAME, keyRequest1, String.class);
    assertEquals(expectedState1, resultGet1.block());
    State<String> keyRequest2 = buildStateKey(null, key2, etag, null);
    Mono<State<String>> resultGet2 = adapter.getState(STATE_STORE_NAME, keyRequest2, String.class);
    assertEquals(expectedState2, resultGet2.block());

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class), any());

    Mono<Void> resultDelete = adapter.deleteState(STATE_STORE_NAME, keyRequest2.getKey(), keyRequest2.getEtag(),
        keyRequest2.getOptions());
    resultDelete.block();
  }

  @Test
  public void getStateNullEtag() throws Exception {
    String etag = null;
    String key1 = "key1";
    String expectedValue1 = "Expected state 1";
    State<String> expectedState1 = buildStateKey(expectedValue1, key1, etag, new HashMap<>(), null);
    Map<String, DaprProtos.GetStateResponse> futuresMap = new HashMap<>();
    DaprProtos.GetStateResponse envelope = DaprProtos.GetStateResponse.newBuilder()
            .setData(serialize(expectedValue1))
            .build();
    futuresMap.put(key1, envelope);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(futuresMap.get(key1));
      observer.onCompleted();
      return null;
    }).when(client).getState(argThat(new GetStateRequestKeyMatcher(key1)), any());

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetBulkStateResponse> observer = (StreamObserver<DaprProtos.GetBulkStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<String>> result = adapter.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();

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

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetSecretRequest req = invocation.getArgument(0);
      assertEquals("key", req.getKey());
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprProtos.GetSecretResponse> observer = (StreamObserver<DaprProtos.GetSecretResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    Map<String, String> result = adapter.getSecret(SECRET_STORE_NAME, "key").block();

    assertEquals(1, result.size());
    assertEquals(expectedValue, result.get(expectedKey));
  }

  @Test
  public void getSecretsEmptyResponse() {
    DaprProtos.GetSecretResponse responseEnvelope = buildGetSecretResponse();

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetSecretRequest req = invocation.getArgument(0);
      assertEquals("key", req.getKey());
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprProtos.GetSecretResponse> observer = (StreamObserver<DaprProtos.GetSecretResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    Map<String, String> result = adapter.getSecret(SECRET_STORE_NAME, "key").block();

    assertTrue(result.isEmpty());
  }

  @Test
  public void getSecretsException() {
    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetSecretRequest req = invocation.getArgument(0);
      assertEquals("key", req.getKey());
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprProtos.GetSecretResponse> observer = (StreamObserver<DaprProtos.GetSecretResponse>) invocation.getArguments()[1];
      observer.onError(new RuntimeException());
      return null;
    }).when(client).getSecret(any(DaprProtos.GetSecretRequest.class), any());

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
    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetSecretRequest req = invocation.getArgument(0);
      assertEquals("key", req.getKey());
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals("metavalue", req.getMetadataMap().get("metakey"));

      StreamObserver<DaprProtos.GetSecretResponse> observer = (StreamObserver<DaprProtos.GetSecretResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(client).getSecret(any(DaprProtos.GetSecretRequest.class), any());

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
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    for (StateOptions.Consistency consistency : StateOptions.Consistency.values()) {
      StateOptions options = buildStateOptions(consistency, StateOptions.Concurrency.FIRST_WRITE);
      Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
      result.block();
    }
  }

  /* If this test is failing, it means that a new value was added to StateOptions.Concurrency
   * enum, without creating a mapping to one of the proto defined gRPC enums
   */
  @Test
  public void stateOptionsConcurrencyValuesHaveValidGrpcEnumMappings() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(client).saveState(any(DaprProtos.SaveStateRequest.class), any());

    for (StateOptions.Concurrency concurrency : StateOptions.Concurrency.values()) {
      StateOptions options = buildStateOptions(StateOptions.Consistency.EVENTUAL, concurrency);
      Mono<Void> result = adapter.saveState(STATE_STORE_NAME, key, etag, value, options);
      result.block();
    }
  }

  private <T> DaprProtos.GetStateResponse buildFutureGetStateEnvelop(T value, String etag) throws IOException {
    return buildGetStateResponse(value, etag);
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
