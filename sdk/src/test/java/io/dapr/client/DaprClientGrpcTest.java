/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.AppConnectionPropertiesHealthMetadata;
import io.dapr.client.domain.AppConnectionPropertiesMetadata;
import io.dapr.client.domain.ComponentMetadata;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DaprMetadata;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.RuleMetadata;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.SubscriptionMetadata;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.dapr.v1.DaprProtos.ActiveActorsCount;
import io.dapr.v1.DaprProtos.ActorRuntime;
import io.dapr.v1.DaprProtos.AppConnectionHealthProperties;
import io.dapr.v1.DaprProtos.AppConnectionProperties;
import io.dapr.v1.DaprProtos.MetadataHTTPEndpoint;
import io.dapr.v1.DaprProtos.PubsubSubscription;
import io.dapr.v1.DaprProtos.PubsubSubscriptionRules;
import io.dapr.v1.DaprProtos.RegisteredComponents;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprClientGrpcTest {

  private static final String STATE_STORE_NAME = "MyStateStore";

  private static final String CONFIG_STORE_NAME = "MyConfigStore";

  private static final String SECRET_STORE_NAME = "MySecretStore";

  private GrpcChannelFacade channel;
  private DaprGrpc.DaprStub daprStub;
  private DaprHttp daprHttp;
  private DaprClient client;
  private ObjectSerializer serializer;

  @BeforeEach
  public void setup() throws IOException {
    channel = mock(GrpcChannelFacade.class);
    daprStub = mock(DaprGrpc.DaprStub.class);
    daprHttp = mock(DaprHttp.class);
    when(daprStub.withInterceptors(any())).thenReturn(daprStub);
    client = new DaprClientImpl(
        channel, daprStub, daprHttp, new DefaultObjectSerializer(), new DefaultObjectSerializer());
    serializer = new ObjectSerializer();
    doNothing().when(channel).close();
  }

  @AfterEach
  public void tearDown() throws Exception {
    client.close();
    verify(channel).close();
  }

  @Test
  public void publishEventExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument");
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    assertThrowsDaprException(
            StatusRuntimeException.class,
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: bad bad argument",
            () -> client.publishEvent("pubsubname","topic", "object").block());
  }

  @Test
  public void publishEventCallbackExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));
      return null;
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    Mono<Void> result = client.publishEvent("pubsubname","topic", "object");

    assertThrowsDaprException(
        ExecutionException.class,
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: bad bad argument",
        () -> result.block());
  }

  @Test
  public void publishEventSerializeException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    client = new DaprClientImpl(channel, daprStub, daprHttp, mockSerializer, new DefaultObjectSerializer());
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    Mono<Void> result = client.publishEvent("pubsubname","topic", "{invalid-json");

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
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

    Mono<Void> result = client.publishEvent("pubsubname","topic", "object");
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
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());
    client.publishEvent("pubsubname", "topic", "object");
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
    }).when(daprStub).publishEvent(ArgumentMatchers.argThat(publishEventRequest -> {
      if (!"application/json".equals(publishEventRequest.getDataContentType())) {
        return false;
      }

      if (!"{\"id\":1,\"value\":\"Event\"}".equals(new String(publishEventRequest.getData().toByteArray())) &&
          !"{\"value\":\"Event\",\"id\":1}".equals(new String(publishEventRequest.getData().toByteArray()))) {
        return false;
      }
      return true;
    }), any());

    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = client.publishEvent("pubsubname", "topic", event);
    result.block();
  }

  @Test
  public void publishEventContentTypeOverrideTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).publishEvent(ArgumentMatchers.argThat(publishEventRequest -> {
      if (!"text/plain".equals(publishEventRequest.getDataContentType())) {
        return false;
      }

      if (!"\"hello\"".equals(new String(publishEventRequest.getData().toByteArray()))) {
        return false;
      }
      return true;
    }), any());


    Mono<Void> result = client.publishEvent(
        new PublishEventRequest("pubsubname", "topic", "hello")
            .setContentType("text/plain"));
    result.block();
  }

  @Test
  public void invokeBindingIllegalArgumentExceptionTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      // empty binding name
      client.invokeBinding("", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null binding name
      client.invokeBinding(null, "MyOperation", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null binding operation
      client.invokeBinding("BindingName", null, "request".getBytes(), Collections.EMPTY_MAP).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty binding operation
      client.invokeBinding("BindingName", "", "request".getBytes(), Collections.EMPTY_MAP).block();
    });
  }

  @Test
  public void invokeBindingSerializeException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    client = new DaprClientImpl(channel, daprStub, daprHttp, mockSerializer, new DefaultObjectSerializer());
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    Mono<Void> result = client.invokeBinding("BindingName", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP);

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = client.invokeBinding("BindingName", "MyOperation", "request");

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = client.invokeBinding("BindingName", "MyOperation", "request");

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<Void> result = client.invokeBinding("BindingName", "MyOperation", "request");
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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    Mono<byte[]> result = client.invokeBinding("BindingName", "MyOperation", "request".getBytes(), Collections.EMPTY_MAP);

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<Void> result = client.invokeBinding("BindingName", "MyOperation", event);

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<String> result = client.invokeBinding("BindingName", "MyOperation", event, String.class);

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());

    MyObject event = new MyObject(1, "Event");
    Mono<String> result = client.invokeBinding("BindingName", "MyOperation", event, TypeRef.get(String.class));

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
    }).when(daprStub).invokeBinding(any(DaprProtos.InvokeBindingRequest.class), any());
    MyObject event = new MyObject(1, "Event");
    client.invokeBinding("BindingName", "MyOperation", event);
    // Do not call block() on mono above, so nothing should happen.
    assertFalse(called.get());
  }

  @Test
  public void getStateIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      client.getState("", key, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.getState(null, key, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      client.getState(STATE_STORE_NAME, (String)null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key
      client.getState(STATE_STORE_NAME, "", String.class).block();
    });
  }

  @Test
  public void getStateExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = client.getState(STATE_STORE_NAME, key, String.class);

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
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<State<String>> result = client.getState(STATE_STORE_NAME, key, String.class);

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
    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> keyRequest = buildStateKey(null, key, etag, null);
    Mono<State<String>> result = client.getState(STATE_STORE_NAME, keyRequest, String.class);
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
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    State<String> keyRequest = buildStateKey(null, key, etag, null);
    client.getState(STATE_STORE_NAME, keyRequest, String.class);
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
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<State<MyObject>> result = client.getState(STATE_STORE_NAME, keyRequest, MyObject.class);
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
    GetStateRequest request = new GetStateRequest(STATE_STORE_NAME, key)
        .setMetadata(metadata)
        .setStateOptions(options);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<State<MyObject>> result = client.getState(request, TypeRef.get(MyObject.class));
    State<MyObject> res = result.block();
    assertNotNull(res);
    assertEquals(expectedState, res);
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
    }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

    Mono<State<MyObject>> result = client.getState(STATE_STORE_NAME, keyRequest, MyObject.class);

    assertEquals(expectedState, result.block());
  }

  @Test
  public void getStatesIllegalArgumentExceptionTest() {
    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      client.getBulkState("", Collections.singletonList("100"), String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.getBulkState(null, Collections.singletonList("100"), String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      // null pointer exception due to keys being converted to an unmodifiable list
      client.getBulkState(STATE_STORE_NAME, null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key list
      client.getBulkState(STATE_STORE_NAME, Collections.emptyList(), String.class).block();
    });
    // negative parallelism
    GetBulkStateRequest req = new GetBulkStateRequest(STATE_STORE_NAME, Collections.singletonList("100"))
        .setMetadata(new HashMap<>())
        .setParallelism(-1);
    assertThrows(IllegalArgumentException.class, () -> client.getBulkState(req, TypeRef.BOOLEAN).block());
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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<String>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();

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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());
    List<State<Integer>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), int.class).block();

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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<Boolean>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), boolean.class).block();

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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<byte[]>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), byte[].class).block();

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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<MyObject>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), MyObject.class).block();

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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, key.getKey(), key.getEtag(), key.getOptions());

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
      client.deleteState("", key.getKey(), "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.deleteState(null, key.getKey(), "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.deleteState(STATE_STORE_NAME, null, "etag", null).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.deleteState(STATE_STORE_NAME, "", "etag", null).block();
    });
  }

  @Test
  public void deleteStateCallbackExcpetionThrownTest() {
    RuntimeException ex = new RuntimeException("An Exception");
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onError(ex);
      return null;
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> key = buildStateKey(null, "Key1", "ETag1", null);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, key.getKey(), key.getEtag(), key.getOptions());

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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, null);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    DeleteStateRequest request = new DeleteStateRequest(STATE_STORE_NAME, key);
    request.setEtag(etag)
        .setStateOptions(options)
        .setMetadata(metadata);
    Mono<Void> result = client.deleteState(request);
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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    client.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
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
    }).when(daprStub).deleteState(any(DaprProtos.DeleteStateRequest.class), any());

    State<String> stateKey = buildStateKey(null, key, etag, options);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, stateKey.getKey(), stateKey.getEtag(),
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
      client.executeStateTransaction("", Collections.singletonList(upsertOperation)).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null state store name
      client.executeStateTransaction(null, Collections.singletonList(upsertOperation)).block();
    });
  }

  @Test
  public void executeTransactionSerializerExceptionTest() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    client = new DaprClientImpl(channel, daprStub, daprHttp, mockSerializer, mockSerializer);
    String etag = "ETag1";
    String key = "key1";
    String data = "my data";
    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());


    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequest(STATE_STORE_NAME)
        .setOperations(Collections.singletonList(upsertOperation));
    Mono<Void> result = client.executeStateTransaction(request);

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
    }).when(daprStub).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("testKey"));
    Map<String, String> metadata = new HashMap<>();
    metadata.put("testKey", "testValue");
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequest(STATE_STORE_NAME)
        .setOperations(Arrays.asList(upsertOperation, deleteOperation))
        .setMetadata(metadata);
    Mono<Void> result = client.executeStateTransaction(request);
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
    }).when(daprStub).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> upsertOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    TransactionalStateOperation<String> deleteOperation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.DELETE,
        new State<>("testKey")
    );
    Mono<Void> result = client.executeStateTransaction(STATE_STORE_NAME, Arrays.asList(upsertOperation, deleteOperation));
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
    }).when(daprStub).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    Mono<Void> result = client.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));

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
    }).when(daprStub).executeStateTransaction(any(DaprProtos.ExecuteStateTransactionRequest.class), any());

    State<String> stateKey = buildStateKey(data, key, etag, options);
    TransactionalStateOperation<String> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT,
        stateKey);
    Mono<Void> result = client.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));

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
      client.saveBulkState("", Collections.emptyList()).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty state store name
      client.saveBulkState(null, Collections.emptyList()).block();
    });
  }

  @Test
  public void saveBulkStateTestNullEtag() {
    List<State<?>> states = new ArrayList<State<?>>();
    states.add(new State<String>("null_etag_key", "null_etag_value", null, (StateOptions)null));
    states.add(new State<String>("empty_etag_key", "empty_etag_value", "", (StateOptions)null));

    ArgumentCaptor<DaprProtos.SaveStateRequest> argument = ArgumentCaptor.forClass(DaprProtos.SaveStateRequest.class);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).saveState(argument.capture(), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.saveBulkState(STATE_STORE_NAME, states);

    result.block();
    assertFalse(argument.getValue().getStates(0).hasEtag());
    assertTrue(argument.getValue().getStates(1).hasEtag());
    assertEquals("", argument.getValue().getStates(1).getEtag().getValue());
  }

  @Test
  public void saveStateExceptionThrownTest() {
    String key = "key1";
    String etag = "ETag1";
    String value = "State value";
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, null);

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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, null);

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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());


    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, null);
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
  }

  @Test
  public void saveStateTestNullEtag() {
    String key = "key1";
    String etag = null;
    String value = "State value";
    ArgumentCaptor<DaprProtos.SaveStateRequest> argument = ArgumentCaptor.forClass(DaprProtos.SaveStateRequest.class);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).saveState(argument.capture(), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);

    result.block();
    assertFalse(argument.getValue().getStates(0).hasEtag());
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    client.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(null, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, null);
    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
    result.block();
  }

  private <T> State<T> buildStateKey(T value, String key, String etag, StateOptions options) {
    return new State<>(key, value, etag, options);
  }

  private <T> State<T> buildStateKey(T value, String key, String etag, Map<String, String> metadata, StateOptions options) {
    return new State<>(key, value, etag, metadata, options);
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
    }).when(daprStub).getState(argThat(new GetStateRequestKeyMatcher(key1)), any());
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetStateResponse> observer = (StreamObserver<DaprProtos.GetStateResponse>) invocation.getArguments()[1];
      observer.onNext(futuresMap.get(key2));
      observer.onCompleted();
      return null;
    }).when(daprStub).getState(argThat(new GetStateRequestKeyMatcher(key2)), any());

    State<String> keyRequest1 = buildStateKey(null, key1, etag, null);
    Mono<State<String>> resultGet1 = client.getState(STATE_STORE_NAME, keyRequest1, String.class);
    assertEquals(expectedState1, resultGet1.block());
    State<String> keyRequest2 = buildStateKey(null, key2, etag, null);
    Mono<State<String>> resultGet2 = client.getState(STATE_STORE_NAME, keyRequest2, String.class);
    assertEquals(expectedState2, resultGet2.block());

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).deleteState(any(io.dapr.v1.DaprProtos.DeleteStateRequest.class), any());

    Mono<Void> resultDelete = client.deleteState(STATE_STORE_NAME, keyRequest2.getKey(), keyRequest2.getEtag(),
        keyRequest2.getOptions());
    resultDelete.block();
  }

  @Test
  public void deleteStateNullEtag() {
    String key = "key1";
    String etag = null;
    ArgumentCaptor<DaprProtos.DeleteStateRequest> argument = ArgumentCaptor.forClass(DaprProtos.DeleteStateRequest.class);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<Empty> observer = (StreamObserver<Empty>) invocation.getArguments()[1];
      observer.onNext(Empty.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).deleteState(argument.capture(), any());

    StateOptions options = buildStateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE);
    Mono<Void> result = client.deleteState(STATE_STORE_NAME, key, etag, options);

    result.block();
    assertFalse(argument.getValue().hasEtag());
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
    }).when(daprStub).getState(argThat(new GetStateRequestKeyMatcher(key1)), any());

    State<String> keyRequest1 = buildStateKey(null, key1, null, null);
    Mono<State<String>> resultGet1 = client.getState(STATE_STORE_NAME, keyRequest1, String.class);
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
    }).when(daprStub).getBulkState(any(DaprProtos.GetBulkStateRequest.class), any());

    List<State<String>> result = client.getBulkState(STATE_STORE_NAME, Arrays.asList("100", "200"), String.class).block();

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
    }).when(daprStub).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    Map<String, String> result = client.getSecret(SECRET_STORE_NAME, "key").block();

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
    }).when(daprStub).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    Map<String, String> result = client.getSecret(SECRET_STORE_NAME, "key").block();

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
    }).when(daprStub).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    assertThrowsDaprException(ExecutionException.class, () -> client.getSecret(SECRET_STORE_NAME, "key").block());
  }

  @Test
  public void getSecretsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> {
      // empty secret store name
      client.getSecret("", "key").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null secret store name
      client.getSecret(null, "key").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // empty key
      client.getSecret(SECRET_STORE_NAME, "").block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      // null key
      client.getSecret(SECRET_STORE_NAME, null).block();
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
    }).when(daprStub).getSecret(any(DaprProtos.GetSecretRequest.class), any());

    Map<String, String> result = client.getSecret(
      SECRET_STORE_NAME,
      "key",
      Collections.singletonMap("metakey", "metavalue")).block();

    assertEquals(1, result.size());
    assertEquals(expectedValue, result.get(expectedKey));
  }

  @Test
  public void getBulkSecrets() {
    DaprProtos.GetBulkSecretResponse responseEnvelope = buildGetBulkSecretResponse(
        new HashMap<String, Map<String, String>>() {{
          put("one", Collections.singletonMap("mysecretkey", "mysecretvalue"));
          put("two", new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
          }});
        }});

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetBulkSecretRequest req = invocation.getArgument(0);
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprProtos.GetBulkSecretResponse> observer =
          (StreamObserver<DaprProtos.GetBulkSecretResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).getBulkSecret(any(DaprProtos.GetBulkSecretRequest.class), any());

    Map<String, Map<String, String>> secrets = client.getBulkSecret(SECRET_STORE_NAME).block();

    assertEquals(2, secrets.size());
    assertEquals(1, secrets.get("one").size());
    assertEquals("mysecretvalue", secrets.get("one").get("mysecretkey"));
    assertEquals(2, secrets.get("two").size());
    assertEquals("1", secrets.get("two").get("a"));
    assertEquals("2", secrets.get("two").get("b"));
  }

  @Test
  public void getBulkSecretsWithMetadata() {
    DaprProtos.GetBulkSecretResponse responseEnvelope = buildGetBulkSecretResponse(
        new HashMap<String, Map<String, String>>() {{
          put("one", Collections.singletonMap("mysecretkey", "mysecretvalue"));
          put("two", new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
          }});
        }});

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.GetBulkSecretRequest req = invocation.getArgument(0);
      assertEquals(SECRET_STORE_NAME, req.getStoreName());
      assertEquals(1, req.getMetadataCount());
      assertEquals("metavalue", req.getMetadataOrThrow("metakey"));

      StreamObserver<DaprProtos.GetBulkSecretResponse> observer =
          (StreamObserver<DaprProtos.GetBulkSecretResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).getBulkSecret(any(DaprProtos.GetBulkSecretRequest.class), any());

    Map<String, Map<String, String>> secrets = client.getBulkSecret(
        SECRET_STORE_NAME, Collections.singletonMap("metakey", "metavalue")).block();

    assertEquals(2, secrets.size());
    assertEquals(1, secrets.get("one").size());
    assertEquals("mysecretvalue", secrets.get("one").get("mysecretkey"));
    assertEquals(2, secrets.get("two").size());
    assertEquals("1", secrets.get("two").get("a"));
    assertEquals("2", secrets.get("two").get("b"));
  }

  @Test
  public void getConfigurationTestErrorScenario() {
    assertThrows(IllegalArgumentException.class, () -> {
      client.getConfiguration("", "key").block();
    });
  }

  @Test
  public void getSingleConfigurationTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetConfigurationResponse> observer =
          (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(getSingleMockResponse());
      observer.onCompleted();
      return null;
    }).when(daprStub).getConfiguration(any(DaprProtos.GetConfigurationRequest.class), any());

    ConfigurationItem ci = client.getConfiguration(CONFIG_STORE_NAME, "configkey1").block();
    assertEquals("configvalue1", ci.getValue());
    assertEquals("1", ci.getVersion());
  }

  @Test
  public void getSingleConfigurationWithMetadataTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetConfigurationResponse> observer =
          (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(getSingleMockResponse());
      observer.onCompleted();
      return null;
    }).when(daprStub).getConfiguration(any(DaprProtos.GetConfigurationRequest.class), any());

    Map<String, String> reqMetadata = new HashMap<>();
    reqMetadata.put("meta1", "value1");
    ConfigurationItem ci = client.getConfiguration(CONFIG_STORE_NAME, "configkey1", reqMetadata).block();
    assertEquals("configvalue1", ci.getValue());
    assertEquals("1", ci.getVersion());
  }

  @Test
  public void getMultipleConfigurationTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetConfigurationResponse> observer =
          (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(getMultipleMockResponse());
      observer.onCompleted();
      return null;
    }).when(daprStub).getConfiguration(any(DaprProtos.GetConfigurationRequest.class), any());

    Map<String, ConfigurationItem> cis = client.getConfiguration(CONFIG_STORE_NAME, "configkey1","configkey2").block();
    assertEquals(2, cis.size());
    assertTrue(cis.containsKey("configkey1"), "configkey1");
    assertEquals("configvalue1", cis.get("configkey1").getValue());
    assertEquals("1", cis.get("configkey1").getVersion());
    assertTrue(cis.containsKey("configkey2"), "configkey2");
    assertEquals("configvalue2", cis.get("configkey2").getValue());
    assertEquals("1", cis.get("configkey2").getVersion());
  }

  @Test
  public void getMultipleConfigurationWithMetadataTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetConfigurationResponse> observer =
          (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(getMultipleMockResponse());
      observer.onCompleted();
      return null;
    }).when(daprStub).getConfiguration(any(DaprProtos.GetConfigurationRequest.class), any());

    Map<String, String> reqMetadata = new HashMap<>();
    reqMetadata.put("meta1", "value1");
    List<String> keys = Arrays.asList("configkey1","configkey2");
    Map<String, ConfigurationItem> cis = client.getConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).block();
    assertEquals(2, cis.size());
    assertTrue(cis.containsKey("configkey1"), "configkey1");
    assertEquals("configvalue1", cis.get("configkey1").getValue());
  }

  @Test
  public void subscribeConfigurationTest() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
    configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
        .setValue("configvalue1")
        .setVersion("1")
        .putAllMetadata(metadata)
        .build());
    DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
        .putAllItems(configs)
        .setId("subscription_id")
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
          (StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).subscribeConfiguration(any(DaprProtos.SubscribeConfigurationRequest.class), any());

    Iterator<SubscribeConfigurationResponse> itr = client.subscribeConfiguration(CONFIG_STORE_NAME, "configkey1").toIterable().iterator();
    assertTrue(itr.hasNext());
    SubscribeConfigurationResponse res = itr.next();
    assertTrue(res.getItems().containsKey("configkey1"));
    assertEquals("subscription_id", res.getSubscriptionId());
    assertFalse(itr.hasNext());
  }

  @Test
  public void subscribeConfigurationTestWithMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
    configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
        .setValue("configvalue1")
        .setVersion("1")
        .putAllMetadata(metadata)
        .build());
    DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
        .putAllItems(configs)
        .setId("subscription_id")
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
          (StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).subscribeConfiguration(any(DaprProtos.SubscribeConfigurationRequest.class), any());

    Map<String, String> reqMetadata = new HashMap<>();
    List<String> keys = Arrays.asList("configkey1");

    Iterator<SubscribeConfigurationResponse> itr = client.subscribeConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).toIterable().iterator();
    assertTrue(itr.hasNext());
    SubscribeConfigurationResponse res = itr.next();
    assertTrue(res.getItems().containsKey("configkey1"));
    assertEquals("subscription_id", res.getSubscriptionId());
    assertFalse(itr.hasNext());
  }

  @Test
  public void subscribeConfigurationWithErrorTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
          (StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
      observer.onError(new RuntimeException());
      observer.onCompleted();
      return null;
    }).when(daprStub).subscribeConfiguration(any(DaprProtos.SubscribeConfigurationRequest.class), any());

    assertThrowsDaprException(ExecutionException.class, () -> {
      client.subscribeConfiguration(CONFIG_STORE_NAME, "key").blockFirst();
    });

    assertThrows(IllegalArgumentException.class, () -> {
      client.subscribeConfiguration("", "key").blockFirst();
    });
  }

  @Test
  public void unsubscribeConfigurationTest() {
    DaprProtos.UnsubscribeConfigurationResponse responseEnvelope = DaprProtos.UnsubscribeConfigurationResponse.newBuilder()
        .setOk(true)
        .setMessage("unsubscribed_message")
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.UnsubscribeConfigurationResponse> observer =
          (StreamObserver<DaprProtos.UnsubscribeConfigurationResponse>) invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).unsubscribeConfiguration(any(DaprProtos.UnsubscribeConfigurationRequest.class), any());

    UnsubscribeConfigurationResponse
        response = client.unsubscribeConfiguration("subscription_id", CONFIG_STORE_NAME).block();
    assertTrue(response.getIsUnsubscribed());
    assertEquals("unsubscribed_message", response.getMessage());
  }

  @Test
  public void unsubscribeConfigurationTestWithError() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.UnsubscribeConfigurationResponse> observer =
          (StreamObserver<DaprProtos.UnsubscribeConfigurationResponse>) invocation.getArguments()[1];
      observer.onError(new RuntimeException());
      observer.onCompleted();
      return null;
    }).when(daprStub).unsubscribeConfiguration(any(DaprProtos.UnsubscribeConfigurationRequest.class), any());

    assertThrowsDaprException(ExecutionException.class, () -> {
      client.unsubscribeConfiguration("subscription_id", CONFIG_STORE_NAME).block();
    });

    assertThrows(IllegalArgumentException.class, () -> {
      client.unsubscribeConfiguration("", CONFIG_STORE_NAME).block();
    });

    UnsubscribeConfigurationRequest req = new UnsubscribeConfigurationRequest("subscription_id", "");
    assertThrows(IllegalArgumentException.class, () -> {
      client.unsubscribeConfiguration(req).block();
    });
  }

  private DaprProtos.GetConfigurationResponse getSingleMockResponse() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
    configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
        .setValue("configvalue1")
        .setVersion("1")
        .putAllMetadata(metadata)
        .build());
    DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
        .putAllItems(configs)
        .build();
    return responseEnvelope;
  }

  private DaprProtos.GetConfigurationResponse getMultipleMockResponse() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("meta1", "value1");
    Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
    configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
        .setValue("configvalue1")
        .setVersion("1")
        .putAllMetadata(metadata)
        .build());
    configs.put("configkey2", CommonProtos.ConfigurationItem.newBuilder()
        .setValue("configvalue2")
        .setVersion("1")
        .putAllMetadata(metadata)
        .build());
    DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
        .putAllItems(configs)
        .build();
    return responseEnvelope;
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    for (StateOptions.Consistency consistency : StateOptions.Consistency.values()) {
      StateOptions options = buildStateOptions(consistency, StateOptions.Concurrency.FIRST_WRITE);
      Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
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
    }).when(daprStub).saveState(any(DaprProtos.SaveStateRequest.class), any());

    for (StateOptions.Concurrency concurrency : StateOptions.Concurrency.values()) {
      StateOptions options = buildStateOptions(StateOptions.Consistency.EVENTUAL, concurrency);
      Mono<Void> result = client.saveState(STATE_STORE_NAME, key, etag, value, options);
      result.block();
    }
  }

  @Test
  public void shutdownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.ShutdownRequest> observer = (StreamObserver<DaprProtos.ShutdownRequest>) invocation.getArguments()[1];
      observer.onNext(DaprProtos.ShutdownRequest.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).shutdown(any(DaprProtos.ShutdownRequest.class), any());

    Mono<Void> result = client.shutdown();
    result.block();
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

  private DaprProtos.GetBulkSecretResponse buildGetBulkSecretResponse(Map<String, Map<String, String>> res) {
    Map<String, DaprProtos.SecretResponse> map = res.entrySet().stream().collect(
        Collectors.toMap(
            e -> e.getKey(),
            e -> DaprProtos.SecretResponse.newBuilder().putAllSecrets(e.getValue()).build()));
    return DaprProtos.GetBulkSecretResponse.newBuilder()
        .putAllData(map)
        .build();
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

  public static StatusRuntimeException newStatusRuntimeException(String statusCode, String message) {
    return new StatusRuntimeException(Status.fromCode(Status.Code.valueOf(statusCode)).withDescription(message));
  }

  public static StatusRuntimeException newStatusRuntimeException(String statusCode, String message, com.google.rpc.Status statusDetails) {
    com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
            .setCode(Status.Code.valueOf(statusCode).value())
            .setMessage(message)
            .addAllDetails(statusDetails.getDetailsList())
            .build();

    return StatusProto.toStatusRuntimeException(status);
  }

  @Test
  public void getMetadataTest() {
    ActiveActorsCount activeActorsCount = DaprProtos.ActiveActorsCount.newBuilder()
        .setType("actor")
        .setCount(1)
        .build();

    ActorRuntime actorRuntime = DaprProtos.ActorRuntime.newBuilder()
        .addActiveActors(activeActorsCount)
        .build();

    RegisteredComponents registeredComponents = DaprProtos.RegisteredComponents.newBuilder()
        .setName("statestore")
        .setType("state.redis")
        .setVersion("v1")
        .build();

    DaprProtos.MetadataHTTPEndpoint httpEndpoint = DaprProtos.MetadataHTTPEndpoint.newBuilder()
        .setName("httpEndpoint")
        .build();

    PubsubSubscriptionRules pubsubSubscriptionRules = DaprProtos.PubsubSubscriptionRules.newBuilder()
        .addRules(DaprProtos.PubsubSubscriptionRule.newBuilder().setPath("/events").build())
        .build();

    PubsubSubscription pubsubSubscription = DaprProtos.PubsubSubscription.newBuilder()
        .setDeadLetterTopic("")
        .setPubsubName("pubsub")
        .setTopic("topic")
        .setRules(pubsubSubscriptionRules)
        .build();

    AppConnectionHealthProperties healthProperties = DaprProtos.AppConnectionHealthProperties.newBuilder()
        .setHealthCheckPath("/health")
        .setHealthProbeInterval("10s")
        .setHealthProbeTimeout("5s")
        .setHealthThreshold(1)
        .build();

    AppConnectionProperties appConnectionProperties = DaprProtos.AppConnectionProperties.newBuilder()
        .setPort(8080)
        .setProtocol("http")
        .setChannelAddress("localhost")
        .setMaxConcurrency(1)
        .setHealth(healthProperties)
        .build();

    DaprProtos.GetMetadataResponse responseEnvelope = DaprProtos.GetMetadataResponse.newBuilder()
        .setId("app")
        .setRuntimeVersion("1.1x.x")
        .addAllEnabledFeatures(Collections.emptyList())
        .setActorRuntime(actorRuntime)
        .putAllExtendedMetadata(Collections.emptyMap())
        .addAllRegisteredComponents(Collections.singletonList(registeredComponents))
        .addAllHttpEndpoints(Collections.singletonList(httpEndpoint))
        .addAllSubscriptions(Collections.singletonList(pubsubSubscription))
        .setAppConnectionProperties(appConnectionProperties)
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprProtos.GetMetadataResponse> observer = (StreamObserver<DaprProtos.GetMetadataResponse>) invocation
          .getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).getMetadata(any(DaprProtos.GetMetadataRequest.class), any());

    Mono<DaprMetadata> result = client.getMetadata();
    DaprMetadata metadata = result.block();

    assertNotNull(metadata);
    assertEquals("app", metadata.getId());
    assertEquals("1.1x.x", metadata.getRuntimeVersion());
    assertEquals(0, metadata.getEnabledFeatures().size());
    assertEquals(0, metadata.getAttributes().size());

    // Actors
    assertEquals(1, metadata.getActors().size());
    assertEquals(activeActorsCount.getType(), metadata.getActors().get(0).getType());
    assertEquals(activeActorsCount.getCount(), metadata.getActors().get(0).getCount());

    // Components
    assertEquals(1, metadata.getComponents().size());

    ComponentMetadata componentMetadata = metadata.getComponents().get(0);

    assertEquals(registeredComponents.getName(), componentMetadata.getName());
    assertEquals(registeredComponents.getVersion(), componentMetadata.getVersion());
    assertEquals(registeredComponents.getType(), componentMetadata.getType());
    assertEquals(registeredComponents.getCapabilitiesList(), componentMetadata.getCapabilities());

    // Subscriptions
    assertEquals(1, metadata.getSubscriptions().size());

    SubscriptionMetadata subscriptionMetadata = metadata.getSubscriptions().get(0);

    assertEquals(pubsubSubscription.getPubsubName(), subscriptionMetadata.getPubsubname());
    assertEquals(pubsubSubscription.getTopic(), subscriptionMetadata.getTopic());
    assertEquals(pubsubSubscription.getDeadLetterTopic(), subscriptionMetadata.getDeadLetterTopic());

    // Subscription Rules
    assertEquals(1, subscriptionMetadata.getRules().size());

    RuleMetadata ruleMetadata = subscriptionMetadata.getRules().get(0);

    assertEquals(pubsubSubscription.getRules().getRules(0).getMatch(), ruleMetadata.getMatch());
    assertEquals(pubsubSubscription.getRules().getRules(0).getPath(), ruleMetadata.getPath());

    // HTTP Endpoints
    assertEquals(1, metadata.getHttpEndpoints().size());
    assertEquals(httpEndpoint.getName(), metadata.getHttpEndpoints().get(0).getName());

    // App Connection Properties
    AppConnectionPropertiesMetadata appConnectionPropertiesMetadata = metadata.getAppConnectionProperties();

    assertEquals(appConnectionProperties.getPort(), appConnectionPropertiesMetadata.getPort());
    assertEquals(appConnectionProperties.getProtocol(), appConnectionPropertiesMetadata.getProtocol());
    assertEquals(appConnectionProperties.getChannelAddress(), appConnectionPropertiesMetadata.getChannelAddress());
    assertEquals(appConnectionProperties.getMaxConcurrency(), appConnectionPropertiesMetadata.getMaxConcurrency());

    // App Connection Health Properties
    AppConnectionPropertiesHealthMetadata healthMetadata = appConnectionPropertiesMetadata.getHealth();

    assertEquals(healthProperties.getHealthCheckPath(), healthMetadata.getHealthCheckPath());
    assertEquals(healthProperties.getHealthProbeInterval(), healthMetadata.getHealthProbeInterval());
    assertEquals(healthProperties.getHealthProbeTimeout(), healthMetadata.getHealthProbeTimeout());
    assertEquals(healthProperties.getHealthThreshold(), healthMetadata.getHealthThreshold());
  }

  @Test
  public void getMetadataExceptionTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw new RuntimeException();
    }).when(daprStub).getMetadata(any(DaprProtos.GetMetadataRequest.class), any());

    Mono<DaprMetadata> result = client.getMetadata();

    assertThrowsDaprException(
        RuntimeException.class,
        "UNKNOWN",
        "UNKNOWN: ",
        () -> result.block());
  }
}
