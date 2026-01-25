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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.dapr.client.domain.AssistantMessage;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolCallsOfFunction;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.DeveloperMessage;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SystemMessage;
import io.dapr.client.domain.ToolMessage;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.UserMessage;
import io.dapr.client.domain.query.Query;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprAiProtos;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprCryptoProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprLockProtos;
import io.dapr.v1.DaprPubsubProtos;
import io.dapr.v1.DaprStateProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DaprPreviewClientGrpcTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String QUERY_STORE_NAME = "testQueryStore";

  private static final String PUBSUB_NAME = "testPubsub";

  private static final String TOPIC_NAME = "testTopic";

  private static final String LOCK_STORE_NAME = "MyLockStore";

  private GrpcChannelFacade channel;
  private DaprGrpc.DaprStub daprStub;
  private DaprHttp daprHttp;
  private DaprPreviewClient previewClient;

  @BeforeEach
  public void setup() throws IOException {
    channel = mock(GrpcChannelFacade.class);
    daprStub = mock(DaprGrpc.DaprStub.class);
    daprHttp = mock(DaprHttp.class);
    when(daprStub.withInterceptors(any())).thenReturn(daprStub);
    previewClient = new DaprClientImpl(
            channel, daprStub, daprHttp, new DefaultObjectSerializer(), new DefaultObjectSerializer());
    doNothing().when(channel).close();
  }

  @AfterEach
  public void tearDown() throws Exception {
    previewClient.close();
    verify(channel).close();
    verifyNoMoreInteractions(channel);
  }

  @Test
  public void publishEventsExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument");
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());

    assertThrowsDaprException(
            StatusRuntimeException.class,
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: bad bad argument",
            () -> previewClient.publishEvents(new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
                    Collections.EMPTY_LIST)).block());
  }

  @Test
  public void publishEventsCallbackExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());

    assertThrowsDaprException(
            ExecutionException.class,
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: bad bad argument",
            () -> previewClient.publishEvents(new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
                    Collections.EMPTY_LIST)).block());
  }

  @Test
  public void publishEventsContentTypeMismatchException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprPubsubProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());


    BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "testEntry"
            , "application/octet-stream", null);
    BulkPublishRequest<String> wrongReq = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
            Collections.singletonList(entry));

    assertThrows(IllegalArgumentException.class, () -> previewClient.publishEvents(wrongReq).block());
  }

  @Test
  public void publishEventsSerializeException() throws IOException {
    DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
    previewClient = new DaprClientImpl(channel, daprStub, daprHttp, mockSerializer, new DefaultObjectSerializer());
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprPubsubProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).publishEvent(any(DaprPubsubProtos.PublishEventRequest.class), any());
    BulkPublishEntry<Map<String, String>> entry = new BulkPublishEntry<>("1", new HashMap<>(),
            "application/json", null);
    BulkPublishRequest<Map<String, String>> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
            Collections.singletonList(entry));
    when(mockSerializer.serialize(any())).thenThrow(IOException.class);
    Mono<BulkPublishResponse<Map<String, String>>> result = previewClient.publishEvents(req);

    assertThrowsDaprException(
            IOException.class,
            "UNKNOWN",
            "UNKNOWN: ",
            () -> result.block());
  }

  @Test
  public void publishEventsTest() {
    doAnswer((Answer<BulkPublishResponse>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprPubsubProtos.BulkPublishResponse.Builder builder = DaprPubsubProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());

    BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "test",
            "text/plain", null);
    BulkPublishRequest<String> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
            Collections.singletonList(entry));
    Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(req);
    BulkPublishResponse res = result.block();
    Assertions.assertNotNull(res);
    assertEquals( 0, res.getFailedEntries().size(), "expected no entry in failed entries list");
  }

  @Test
  public void publishEventsWithoutMetaTest() {
    doAnswer((Answer<BulkPublishResponse>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprPubsubProtos.BulkPublishResponse.Builder builder = DaprPubsubProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());

    Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(PUBSUB_NAME, TOPIC_NAME,
            "text/plain", Collections.singletonList("test"));
    BulkPublishResponse<String> res = result.block();
    Assertions.assertNotNull(res);
    assertEquals( 0, res.getFailedEntries().size(), "expected no entries in failed entries list");
  }

  @Test
  public void publishEventsWithRequestMetaTest() {
    doAnswer((Answer<BulkPublishResponse>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprPubsubProtos.BulkPublishResponse.Builder builder = DaprPubsubProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprPubsubProtos.BulkPublishRequest.class), any());

    Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(PUBSUB_NAME, TOPIC_NAME,
            "text/plain", new HashMap<String, String>(){{
              put("ttlInSeconds", "123");
            }}, Collections.singletonList("test"));
    BulkPublishResponse<String> res = result.block();
    Assertions.assertNotNull(res);
    assertEquals( 0, res.getFailedEntries().size(), "expected no entry in failed entries list");
  }

  @Test
  public void publishEventsObjectTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprPubsubProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
      DaprPubsubProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
      if (!"application/json".equals(bulkPublishRequest.getEntries(0).getContentType())) {
        return false;
      }

      if (!"{\"id\":1,\"value\":\"Event\"}".equals(new String(entry.getEvent().toByteArray())) &&
              !"{\"value\":\"Event\",\"id\":1}".equals(new String(entry.getEvent().toByteArray()))) {
        return false;
      }
      return true;
    }), any());


    DaprClientGrpcTest.MyObject event = new DaprClientGrpcTest.MyObject(1, "Event");
    BulkPublishEntry<DaprClientGrpcTest.MyObject> entry = new BulkPublishEntry<>("1", event,
            "application/json", null);
    BulkPublishRequest<DaprClientGrpcTest.MyObject> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
            Collections.singletonList(entry));
    BulkPublishResponse<DaprClientGrpcTest.MyObject> result = previewClient.publishEvents(req).block();
    Assertions.assertNotNull(result);
    Assertions.assertEquals(0, result.getFailedEntries().size(), "expected no entries to be failed");
  }

  @Test
  public void publishEventsContentTypeOverrideTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprPubsubProtos.BulkPublishResponse> observer =
          (StreamObserver<DaprPubsubProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprPubsubProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
      DaprPubsubProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
      if (!"application/json".equals(entry.getContentType())) {
        return false;
      }

      if (!"\"hello\"".equals(new String(entry.getEvent().toByteArray()))) {
        return false;
      }
      return true;
    }), any());

    BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "hello",
            "", null);
    BulkPublishRequest<String> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
            Collections.singletonList(entry));
    BulkPublishResponse<String> result = previewClient.publishEvents(req).block();
    Assertions.assertNotNull(result);
    Assertions.assertEquals( 0, result.getFailedEntries().size(), "expected no entries to be failed");
  }

  @Test
  public void queryStateExceptionsTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState("", "query", String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState("storeName", "", String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState("storeName", (Query) null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState("storeName", (String) null, String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState(new QueryStateRequest("storeName"), String.class).block();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.queryState(null, String.class).block();
    });
  }

  @Test
  public void queryState() throws JsonProcessingException {
    List<QueryStateItem<?>> resp = new ArrayList<>();
    resp.add(new QueryStateItem<Object>("1", (Object)"testData", "6f54ad94-dfb9-46f0-a371-e42d550adb7d"));
    DaprStateProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
    doAnswer(invocation -> {
      DaprStateProtos.QueryStateRequest req = (DaprStateProtos.QueryStateRequest) invocation.getArgument(0);
      assertEquals(QUERY_STORE_NAME, req.getStoreName());
      assertEquals("query", req.getQuery());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprStateProtos.QueryStateResponse> observer = (StreamObserver<DaprStateProtos.QueryStateResponse>)
              invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).queryStateAlpha1(any(DaprStateProtos.QueryStateRequest.class), any());

    QueryStateResponse<String> response = previewClient.queryState(QUERY_STORE_NAME, "query", String.class).block();
    assertNotNull(response);
    assertEquals(1, response.getResults().size(), "result size must be 1");
    assertEquals("1", response.getResults().get(0).getKey(), "result must be same");
    assertEquals("testData", response.getResults().get(0).getValue(), "result must be same");
    assertEquals("6f54ad94-dfb9-46f0-a371-e42d550adb7d", response.getResults().get(0).getEtag(), "result must be same");
  }

  @Test
  public void queryStateMetadataError() throws JsonProcessingException {
    List<QueryStateItem<?>> resp = new ArrayList<>();
    resp.add(new QueryStateItem<Object>("1", null, "error data"));
    DaprStateProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
    doAnswer(invocation -> {
      DaprStateProtos.QueryStateRequest req = (DaprStateProtos.QueryStateRequest) invocation.getArgument(0);
      assertEquals(QUERY_STORE_NAME, req.getStoreName());
      assertEquals("query", req.getQuery());
      assertEquals(1, req.getMetadataCount());
      assertEquals(1, req.getMetadataCount());

      StreamObserver<DaprStateProtos.QueryStateResponse> observer = (StreamObserver<DaprStateProtos.QueryStateResponse>)
              invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).queryStateAlpha1(any(DaprStateProtos.QueryStateRequest.class), any());

    QueryStateResponse<String> response = previewClient.queryState(QUERY_STORE_NAME, "query",
            new HashMap<String, String>(){{ put("key", "error"); }}, String.class).block();
    assertNotNull(response);
    assertEquals(1, response.getResults().size(), "result size must be 1");
    assertEquals( "1", response.getResults().get(0).getKey(), "result must be same");
    assertEquals( "error data", response.getResults().get(0).getError(), "result must be same");
  }

  @Test
  public void tryLock() {

    DaprLockProtos.TryLockResponse.Builder builder = DaprLockProtos.TryLockResponse.newBuilder()
            .setSuccess(true);

    DaprLockProtos.TryLockResponse response = builder.build();

    doAnswer((Answer<Void>) invocation -> {
      DaprLockProtos.TryLockRequest req = invocation.getArgument(0);
      assertEquals(LOCK_STORE_NAME, req.getStoreName());
      assertEquals("1", req.getResourceId());
      assertEquals("owner", req.getLockOwner());
      assertEquals(10, req.getExpiryInSeconds());

      StreamObserver<DaprLockProtos.TryLockResponse> observer =
          (StreamObserver<DaprLockProtos.TryLockResponse>) invocation.getArguments()[1];
      observer.onNext(response);
      observer.onCompleted();
      return null;
    }).when(daprStub).tryLockAlpha1(any(DaprLockProtos.TryLockRequest.class), any());

    Boolean result = previewClient.tryLock("MyLockStore", "1", "owner", 10).block();
    assertEquals(Boolean.TRUE, result);
  }

  @Test
  public void unLock() {

    DaprLockProtos.UnlockResponse.Builder builder = DaprLockProtos.UnlockResponse.newBuilder()
        .setStatus(DaprLockProtos.UnlockResponse.Status.SUCCESS);

    DaprLockProtos.UnlockResponse response = builder.build();

    doAnswer((Answer<Void>) invocation -> {
      DaprLockProtos.UnlockRequest req = invocation.getArgument(0);
      assertEquals(LOCK_STORE_NAME, req.getStoreName());
      assertEquals("1", req.getResourceId());
      assertEquals("owner", req.getLockOwner());

      StreamObserver<DaprLockProtos.UnlockResponse> observer =
          (StreamObserver<DaprLockProtos.UnlockResponse>) invocation.getArguments()[1];
      observer.onNext(response);
      observer.onCompleted();
      return null;
    }).when(daprStub).unlockAlpha1(any(DaprLockProtos.UnlockRequest.class), any());

    UnlockResponseStatus result = previewClient.unlock("MyLockStore", "1", "owner").block();
    assertEquals(UnlockResponseStatus.SUCCESS, result);
  }

  @Test
  public void subscribeEventTest() throws Exception {
    var numEvents = 100;
    var numErrors = 3;
    var numDrops = 2;

    var pubsubName = "pubsubName";
    var topicName = "topicName";
    var data = "my message";

    var started = new Semaphore(0);

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) invocation -> {
      StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1> observer =
          (StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>) invocation.getArguments()[0];
      var emitterThread = new Thread(() -> {
        try {
          started.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        observer.onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.getDefaultInstance());
        for (int i = 0; i < numEvents; i++) {
          observer.onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
                  .setEventMessage(DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                          .setId(Integer.toString(i))
                          .setPubsubName(pubsubName)
                          .setTopic(topicName)
                          .setData(ByteString.copyFromUtf8("\"" + data + "\""))
                          .setDataContentType("application/json")
                          .build())
                  .build());
        }

        for (int i = 0; i < numDrops; i++) {
          // Bad messages
          observer.onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
                  .setEventMessage(DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                          .setId(UUID.randomUUID().toString())
                          .setPubsubName("bad pubsub")
                          .setTopic("bad topic")
                          .setData(ByteString.copyFromUtf8("\"\""))
                          .setDataContentType("application/json")
                          .build())
                  .build());
        }
        observer.onCompleted();
      });
      emitterThread.start();
      return new StreamObserver<>() {

        @Override
        public void onNext(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 subscribeTopicEventsRequestAlpha1) {
          started.release();
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }
      };
    }).when(daprStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    final Set<String> success = Collections.synchronizedSet(new HashSet<>());
    final Set<String> errors = Collections.synchronizedSet(new HashSet<>());
    final AtomicInteger dropCounter = new AtomicInteger();
    final Semaphore gotAll = new Semaphore(0);

    final AtomicInteger errorsToBeEmitted = new AtomicInteger(numErrors);

    var subscription = previewClient.subscribeToEvents(
            "pubsubname",
            "topic",
            new SubscriptionListener<>() {
              @Override
              public Mono<Status> onEvent(CloudEvent<String> event) {
                if (event.getPubsubName().equals(pubsubName) &&
                        event.getTopic().equals(topicName) &&
                        event.getData().equals(data)) {

                  // Simulate an error
                  if ((success.size() == 4 /* some random entry */) && errorsToBeEmitted.decrementAndGet() >= 0) {
                    throw new RuntimeException("simulated exception on event " + event.getId());
                  }

                  success.add(event.getId());
                  if (success.size() >= numEvents) {
                    gotAll.release();
                  }
                  return Mono.just(Status.SUCCESS);
                }

                dropCounter.incrementAndGet();
                return Mono.just(Status.DROP);
              }

              @Override
              public void onError(RuntimeException exception) {
                errors.add(exception.getMessage());
              }

            },
            TypeRef.STRING);

    gotAll.acquire();
    subscription.close();

    assertEquals(numEvents, success.size());
    assertEquals(numDrops, dropCounter.get());
    assertEquals(numErrors, errors.size());
  }

  @Test
  public void subscribeEventFluxTest() throws Exception {
    var numEvents = 100;
    var pubsubName = "pubsubName";
    var topicName = "topicName";
    var data = "my message";
    var started = new Semaphore(0);

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) invocation -> {
      StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1> observer =
          (StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>) invocation.getArguments()[0];

      var emitterThread = new Thread(() -> {
        try {
          started.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        observer.onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.getDefaultInstance());

        for (int i = 0; i < numEvents; i++) {
          DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 reponse =
              DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
                  .setEventMessage(DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                      .setId(Integer.toString(i))
                      .setPubsubName(pubsubName)
                      .setTopic(topicName)
                      .setData(ByteString.copyFromUtf8("\"" + data + "\""))
                      .setDataContentType("application/json")
                      .build())
                  .build();
          observer.onNext(reponse);
        }

        observer.onCompleted();
      });

      emitterThread.start();

      return new StreamObserver<>() {
        @Override
        public void onNext(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 subscribeTopicEventsRequestAlpha1) {
          started.release();
        }

        @Override
        public void onError(Throwable throwable) {
          // No-op
        }

        @Override
        public void onCompleted() {
          // No-op
        }
      };
    }).when(daprStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    final AtomicInteger eventCount = new AtomicInteger(0);
    final Semaphore gotAll = new Semaphore(0);

    // subscribeToTopic returns Flux<T> directly (raw data)
    var disposable = previewClient.subscribeToTopic(pubsubName, topicName, TypeRef.STRING)
            .doOnNext(rawData -> {
              // rawData is String directly, not CloudEvent
              assertEquals(data, rawData);
              assertTrue(rawData instanceof String);

              int count = eventCount.incrementAndGet();

              if (count >= numEvents) {
                gotAll.release();
              }
            })
            .subscribe();

    gotAll.acquire();
    disposable.dispose();

    assertEquals(numEvents, eventCount.get());
  }

  @Test
  public void subscribeEventsWithMetadataTest() throws Exception {
    var numEvents = 10;
    var pubsubName = "pubsubName";
    var topicName = "topicName";
    var data = "my message";
    var started = new Semaphore(0);
    var capturedMetadata = new AtomicReference<java.util.Map<String, String>>();

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) invocation -> {
      StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1> observer =
          (StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>) invocation.getArguments()[0];

      var emitterThread = new Thread(() -> {
        try {
          started.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        observer.onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.getDefaultInstance());

        for (int i = 0; i < numEvents; i++) {
          DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response =
              DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
                  .setEventMessage(DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                      .setId(Integer.toString(i))
                      .setPubsubName(pubsubName)
                      .setTopic(topicName)
                      .setData(ByteString.copyFromUtf8("\"" + data + "\""))
                      .setDataContentType("application/json")
                      .build())
                  .build();
          observer.onNext(response);
        }

        observer.onCompleted();
      });

      emitterThread.start();

      return new StreamObserver<>() {
        @Override
        public void onNext(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request) {
          // Capture metadata from initial request
          if (request.hasInitialRequest()) {
            capturedMetadata.set(request.getInitialRequest().getMetadataMap());
          }
          started.release();
        }

        @Override
        public void onError(Throwable throwable) {
          // No-op
        }

        @Override
        public void onCompleted() {
          // No-op
        }
      };
    }).when(daprStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    final AtomicInteger eventCount = new AtomicInteger(0);
    final Semaphore gotAll = new Semaphore(0);
    Map<String, String> metadata = Map.of("rawPayload", "true");

    // Use subscribeToTopic with rawPayload metadata
    var disposable = previewClient.subscribeToTopic(pubsubName, topicName, TypeRef.STRING, metadata)
            .doOnNext(rawData -> {
              assertEquals(data, rawData);
              assertTrue(rawData instanceof String);

              int count = eventCount.incrementAndGet();

              if (count >= numEvents) {
                gotAll.release();
              }
            })
            .subscribe();

    gotAll.acquire();
    disposable.dispose();

    assertEquals(numEvents, eventCount.get());

    // Verify metadata was passed to gRPC request
    assertNotNull(capturedMetadata.get());
    assertEquals("true", capturedMetadata.get().get("rawPayload"));
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenComponentNameIsNull() throws Exception {
    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput("Hello there !"));

    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest(null, inputs)).block());
    assertEquals("LLM name cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationComponentIsEmpty() throws Exception {
    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput("Hello there !"));

    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest("", inputs)).block());
    assertEquals("LLM name cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenInputsIsEmpty() throws Exception {
    List<ConversationInput> inputs = new ArrayList<>();

    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest("openai", inputs)).block());
    assertEquals("Conversation inputs cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenInputsIsNull() throws Exception {
    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest("openai", null)).block());
    assertEquals("Conversation inputs cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenInputContentIsNull() throws Exception {
    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput(null));

    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest("openai", inputs)).block());
    assertEquals("Conversation input content cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenInputContentIsEmpty() throws Exception {
    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput(""));

    IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () ->
                    previewClient.converse(new ConversationRequest("openai", inputs)).block());
    assertEquals("Conversation input content cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseShouldReturnConversationResponseWhenRequiredInputsAreValid() throws Exception {
    DaprAiProtos.ConversationResponse conversationResponse = DaprAiProtos.ConversationResponse.newBuilder()
        .addOutputs(DaprAiProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprAiProtos.ConversationRequest.class), any());

    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput("Hello there"));
    ConversationResponse response =
            previewClient.converse(new ConversationRequest("openai", inputs)).block();

    ArgumentCaptor<DaprAiProtos.ConversationRequest> captor =
        ArgumentCaptor.forClass(DaprAiProtos.ConversationRequest.class);
    verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

    DaprAiProtos.ConversationRequest conversationRequest = captor.getValue();

    assertEquals("openai", conversationRequest.getName());
    assertEquals("Hello there", conversationRequest.getInputs(0).getContent());
    assertEquals("Hello How are you",
            response.getConversationOutputs().get(0).getResult());
  }

  @Test
  public void converseShouldReturnConversationResponseWhenRequiredAndOptionalInputsAreValid() throws Exception {
    DaprAiProtos.ConversationResponse conversationResponse = DaprAiProtos.ConversationResponse.newBuilder()
            .setContextID("contextId")
        .addOutputs(DaprAiProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprAiProtos.ConversationRequest.class), any());

    ConversationInput daprConversationInput = new ConversationInput("Hello there")
            .setRole("Assistant")
            .setScrubPii(true);

    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(daprConversationInput);

    ConversationResponse response =
            previewClient.converse(new ConversationRequest("openai", inputs)
                    .setContextId("contextId")
                    .setScrubPii(true)
                    .setTemperature(1.1d)).block();

    ArgumentCaptor<DaprAiProtos.ConversationRequest> captor =
        ArgumentCaptor.forClass(DaprAiProtos.ConversationRequest.class);
    verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

    DaprAiProtos.ConversationRequest conversationRequest = captor.getValue();

    assertEquals("openai", conversationRequest.getName());
    assertEquals("contextId", conversationRequest.getContextID());
    assertTrue(conversationRequest.getScrubPII());
    assertEquals(1.1d, conversationRequest.getTemperature(), 0d);
    assertEquals("Hello there", conversationRequest.getInputs(0).getContent());
    assertTrue(conversationRequest.getInputs(0).getScrubPII());
    assertEquals("Assistant", conversationRequest.getInputs(0).getRole());
    assertEquals("contextId", response.getContextId());
    assertEquals("Hello How are you",
            response.getConversationOutputs().get(0).getResult());
  }

  @Test
  public void converseAlpha2ShouldThrowIllegalArgumentExceptionWhenNameIsNull() {
    List<ConversationMessage> messages = new ArrayList<>();
    SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("System info")));
    systemMsg.setName("system");
    messages.add(systemMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2(null, List.of(input));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        previewClient.converseAlpha2(request).block());
    assertEquals("LLM name cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseAlpha2ShouldThrowIllegalArgumentExceptionWhenNameIsEmpty() {
    List<ConversationMessage> messages = new ArrayList<>();
    SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("System info")));
    systemMsg.setName("system");
    messages.add(systemMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("", List.of(input));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        previewClient.converseAlpha2(request).block());
    assertEquals("LLM name cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseAlpha2ShouldThrowIllegalArgumentExceptionWhenNameIsWhitespace() {
    List<ConversationMessage> messages = new ArrayList<>();
    SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("System info")));
    systemMsg.setName("system");
    messages.add(systemMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("   ", null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        previewClient.converseAlpha2(request).block());
    assertEquals("LLM name cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseAlpha2ShouldThrowIllegalArgumentExceptionWhenInputIsNull() {
    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("abc", null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            previewClient.converseAlpha2(request).block());
    assertEquals("Conversation Inputs cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseAlpha2ShouldThrowIllegalArgumentExceptionWhenInputIsEmpty() {
    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("abc", new ArrayList<>());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            previewClient.converseAlpha2(request).block());
    assertEquals("Conversation Inputs cannot be null or empty.", exception.getMessage());
  }

  @Test
  public void converseAlpha2ExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      throw newStatusRuntimeException("INVALID_ARGUMENT", "bad argument");
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", null);

    assertThrows(IllegalArgumentException.class, () -> previewClient.converseAlpha2(request).block());
  }

  @Test
  public void converseAlpha2CallbackExceptionThrownTest() {
    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad argument"));
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    List<ConversationMessage> messages = new ArrayList<>();
    SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("System info")));
    systemMsg.setName("system");
    messages.add(systemMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));
    Mono<ConversationResponseAlpha2> result = previewClient.converseAlpha2(request);

    assertThrowsDaprException(
        ExecutionException.class,
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: bad argument",
        () -> result.block());
  }

  @Test
  public void converseAlpha2MinimalRequestTest() {
    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .setContextId("test-context")
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("stop")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("Hello! How can I help you today?")
                    .build())
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    List<ConversationMessage> messages = new ArrayList<>();
    DeveloperMessage devMsg = new DeveloperMessage(List.of(new ConversationMessageContent("Debug info")));
    devMsg.setName("developer");
    messages.add(devMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));
    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    assertEquals("test-context", response.getContextId());
    assertEquals(1, response.getOutputs().size());

    ConversationResultAlpha2 result = response.getOutputs().get(0);
    assertEquals(1, result.getChoices().size());

    ConversationResultChoices choice = result.getChoices().get(0);
    assertEquals("stop", choice.getFinishReason());
    assertEquals(0, choice.getIndex());
    assertEquals("Hello! How can I help you today?", choice.getMessage().getContent());
  }

  @Test
  public void converseAlpha2ComplexRequestTest() {
    // Create messages
    List<ConversationMessage> messages = new ArrayList<>();
    UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("Hello, how are you?")));
    userMessage.setName("John");
    messages.add(userMessage);

    // Create input
    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);
    input.setScrubPii(true);

    // Create tools
    Map<String, Object> functionParams = new HashMap<>();
    functionParams.put("location", "Required location parameter");
    List<ConversationTools> tools = new ArrayList<>();
    ConversationToolsFunction function = new ConversationToolsFunction("get_weather", functionParams);
    function.setDescription("Get current weather");

    ConversationTools tool = new ConversationTools(function);
    tools.add(tool);

    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", "value1");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("max_tokens", "1000");

    Struct responseFormat = Struct.newBuilder().putFields("type",
        Value.newBuilder().setStringValue("text").build()).build();

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));
    request.setContextId("test-context");
    request.setTemperature(0.7);
    request.setScrubPii(true);
    request.setTools(tools);
    request.setToolChoice("auto");
    request.setMetadata(metadata);
    request.setParameters(parameters);
    request.setPromptCacheRetention(Duration.ofDays(1));
    request.setResponseFormat(responseFormat);

    // Mock response with tool calls
    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .setContextId("test-context")
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .setModel("gpt-3.5-turbo")
            .setUsage(DaprAiProtos.ConversationResultAlpha2CompletionUsage.newBuilder()
                .setPromptTokens(100)
                .setCompletionTokens(100)
                .setTotalTokens(200)
                .setCompletionTokensDetails(DaprAiProtos.ConversationResultAlpha2CompletionUsageCompletionTokensDetails
                    .newBuilder()
                    .setAudioTokens(10)
                    .setReasoningTokens(11)
                    .setAcceptedPredictionTokens(222)
                    .setRejectedPredictionTokens(321)
                    .build())
                .setPromptTokensDetails(DaprAiProtos.ConversationResultAlpha2CompletionUsagePromptTokensDetails
                    .newBuilder()
                    .setAudioTokens(654)
                    .setCachedTokens(1112)
                    .build())
                .build())
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("tool_calls")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("I'll help you get the weather information.")
                    .addToolCalls(DaprAiProtos.ConversationToolCalls.newBuilder()
                        .setId("call_123")
                        .setFunction(DaprAiProtos.ConversationToolCallsOfFunction.newBuilder()
                            .setName("get_weather")
                            .setArguments("{\"location\": \"New York\"}")
                            .build())
                        .build())
                    .build())
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    assertEquals("test-context", response.getContextId());

    ConversationResultChoices choice = response.getOutputs().get(0).getChoices().get(0);
    assertEquals("tool_calls", choice.getFinishReason());
    assertEquals("I'll help you get the weather information.", choice.getMessage().getContent());
    assertEquals(1, choice.getMessage().getToolCalls().size());
    assertEquals("gpt-3.5-turbo", response.getOutputs().get(0).getModel());
    assertEquals(100, response.getOutputs().get(0).getUsage().getCompletionTokens());
    assertEquals(100, response.getOutputs().get(0).getUsage().getPromptTokens());
    assertEquals(200, response.getOutputs().get(0).getUsage().getTotalTokens());
    assertEquals(10, response.getOutputs().get(0).getUsage().getCompletionTokenDetails().getAudioTokens());
    assertEquals(11, response.getOutputs().get(0).getUsage().getCompletionTokenDetails().getReasoningTokens());
    assertEquals(222, response.getOutputs().get(0).getUsage().getCompletionTokenDetails().getAcceptedPredictionTokens());
    assertEquals(321, response.getOutputs().get(0).getUsage().getCompletionTokenDetails().getRejectedPredictionTokens());
    assertEquals(654, response.getOutputs().get(0).getUsage().getPromptTokenDetails().getAudioTokens());
    assertEquals(1112, response.getOutputs().get(0).getUsage().getPromptTokenDetails().getCachedTokens());


    ConversationToolCalls toolCall = choice.getMessage().getToolCalls().get(0);
    assertEquals("call_123", toolCall.getId());
    assertEquals("get_weather", toolCall.getFunction().getName());
    assertEquals("{\"location\": \"New York\"}", toolCall.getFunction().getArguments());

    // Verify the request was built correctly
    ArgumentCaptor<DaprAiProtos.ConversationRequestAlpha2> captor =
        ArgumentCaptor.forClass(DaprAiProtos.ConversationRequestAlpha2.class);
    verify(daprStub).converseAlpha2(captor.capture(), any());

    DaprAiProtos.ConversationRequestAlpha2 capturedRequest = captor.getValue();
    assertEquals("openai", capturedRequest.getName());
    assertEquals("test-context", capturedRequest.getContextId());
    assertEquals(0.7, capturedRequest.getTemperature(), 0.001);
    assertTrue(capturedRequest.getScrubPii());
    assertEquals("auto", capturedRequest.getToolChoice());
    assertEquals("value1", capturedRequest.getMetadataMap().get("key1"));
    assertEquals(1, capturedRequest.getToolsCount());
    assertEquals("get_weather", capturedRequest.getTools(0).getFunction().getName());
    assertEquals(Struct.newBuilder().putFields("type",
            Value.newBuilder().setStringValue("text").build()).build(),
        capturedRequest.getResponseFormat());
    assertEquals(Duration.ofDays(1).getSeconds(), capturedRequest.getPromptCacheRetention().getSeconds());
    assertEquals(0, capturedRequest.getPromptCacheRetention().getNanos());
  }

  @Test
  public void converseAlpha2AllMessageTypesTest() {
    List<ConversationMessage> messages = new ArrayList<>();

    // System message
    SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("You are a helpful assistant.")));
    systemMsg.setName("system");
    messages.add(systemMsg);

    // User message
    UserMessage userMsg = new UserMessage(List.of(new ConversationMessageContent("Hello!")));
    userMsg.setName("user");
    messages.add(userMsg);

    // Assistant message
    AssistantMessage assistantMsg = new AssistantMessage(List.of(new ConversationMessageContent("Hi there!")),
        List.of(new ConversationToolCalls(new ConversationToolCallsOfFunction("abc", "parameters"))));
    assistantMsg.setName("assistant");
    messages.add(assistantMsg);

    // Tool message
    ToolMessage toolMsg = new ToolMessage(List.of(new ConversationMessageContent("Weather data: 72F")));
    toolMsg.setName("tool");
    messages.add(toolMsg);

    // Developer message
    DeveloperMessage devMsg = new DeveloperMessage(List.of(new ConversationMessageContent("Debug info")));
    devMsg.setName("developer");
    messages.add(devMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);
    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));

    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("stop")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("Processed all message types")
                    .build())
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    assertEquals("Processed all message types", response.getOutputs().get(0).getChoices().get(0).getMessage().getContent());

    // Verify all message types were processed
    ArgumentCaptor<DaprAiProtos.ConversationRequestAlpha2> captor =
        ArgumentCaptor.forClass(DaprAiProtos.ConversationRequestAlpha2.class);
    verify(daprStub).converseAlpha2(captor.capture(), any());

    DaprAiProtos.ConversationRequestAlpha2 capturedRequest = captor.getValue();
    assertEquals(1, capturedRequest.getInputsCount());
    assertEquals(5, capturedRequest.getInputs(0).getMessagesCount());

    // Verify each message type was converted correctly
    List<DaprAiProtos.ConversationMessage> capturedMessages = capturedRequest.getInputs(0).getMessagesList();
    assertTrue(capturedMessages.get(0).hasOfSystem());
    assertTrue(capturedMessages.get(1).hasOfUser());
    assertTrue(capturedMessages.get(2).hasOfAssistant());
    assertTrue(capturedMessages.get(3).hasOfTool());
    assertTrue(capturedMessages.get(4).hasOfDeveloper());
  }

  @Test
  public void converseAlpha2ResponseWithoutMessageTest() {
    List<ConversationMessage> messages = new ArrayList<>();
    DeveloperMessage devMsg = new DeveloperMessage(List.of(new ConversationMessageContent("Debug info")));
    devMsg.setName("developer");
    messages.add(devMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));

    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("stop")
                .setIndex(0)
                // No message set
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    ConversationResultChoices choice = response.getOutputs().get(0).getChoices().get(0);
    assertEquals("stop", choice.getFinishReason());
    assertEquals(0, choice.getIndex());
    assertNull(choice.getMessage());
  }

  @Test
  public void converseAlpha2MultipleResultsTest() {
    List<ConversationMessage> messages = new ArrayList<>();
    DeveloperMessage devMsg = new DeveloperMessage(List.of(new ConversationMessageContent("Debug info")));
    devMsg.setName("developer");
    messages.add(devMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));

    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("stop")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("First choice")
                    .build())
                .build())
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("stop")
                .setIndex(1)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("Second choice")
                    .build())
                .build())
            .build())
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("length")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("Third result")
                    .build())
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    assertEquals(2, response.getOutputs().size());

    // First result with 2 choices
    ConversationResultAlpha2 firstResult = response.getOutputs().get(0);
    assertEquals(2, firstResult.getChoices().size());
    assertEquals("First choice", firstResult.getChoices().get(0).getMessage().getContent());
    assertEquals("Second choice", firstResult.getChoices().get(1).getMessage().getContent());

    // Second result with 1 choice
    ConversationResultAlpha2 secondResult = response.getOutputs().get(1);
    assertEquals(1, secondResult.getChoices().size());
    assertEquals("Third result", secondResult.getChoices().get(0).getMessage().getContent());
  }

  @Test
  public void converseAlpha2ToolCallWithoutFunctionTest() {
    List<ConversationMessage> messages = new ArrayList<>();
    UserMessage userMsg = new UserMessage(List.of(new ConversationMessageContent("Debug info")));
    userMsg.setName("developer");
    messages.add(userMsg);

    ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2("openai", List.of(input));
    DaprAiProtos.ConversationResponseAlpha2 grpcResponse = DaprAiProtos.ConversationResponseAlpha2.newBuilder()
        .addOutputs(DaprAiProtos.ConversationResultAlpha2.newBuilder()
            .addChoices(DaprAiProtos.ConversationResultChoices.newBuilder()
                .setFinishReason("tool_calls")
                .setIndex(0)
                .setMessage(DaprAiProtos.ConversationResultMessage.newBuilder()
                    .setContent("Test content")
                    .addToolCalls(DaprAiProtos.ConversationToolCalls.newBuilder()
                        .setId("call_123")
                        // No function set
                        .build())
                    .build())
                .build())
            .build())
        .build();

    doAnswer((Answer<Void>) invocation -> {
      StreamObserver<DaprAiProtos.ConversationResponseAlpha2> observer =
          (StreamObserver<DaprAiProtos.ConversationResponseAlpha2>) invocation.getArguments()[1];
      observer.onNext(grpcResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha2(any(DaprAiProtos.ConversationRequestAlpha2.class), any());

    ConversationResponseAlpha2 response = previewClient.converseAlpha2(request).block();

    assertNotNull(response);
    ConversationToolCalls toolCall = response.getOutputs().get(0).getChoices().get(0)
        .getMessage().getToolCalls().get(0);
    assertEquals("call_123", toolCall.getId());
    assertNull(toolCall.getFunction());
  }

  private DaprStateProtos.QueryStateResponse buildQueryStateResponse(List<QueryStateItem<?>> resp, String token)
          throws JsonProcessingException {
    List<DaprStateProtos.QueryStateItem> items = new ArrayList<>();
    for (QueryStateItem<?> item: resp) {
      items.add(buildQueryStateItem(item));
    }
    return DaprStateProtos.QueryStateResponse.newBuilder()
            .addAllResults(items)
            .setToken(token)
            .build();
  }

  private DaprStateProtos.QueryStateItem buildQueryStateItem(QueryStateItem<?> item) throws JsonProcessingException {
    DaprStateProtos.QueryStateItem.Builder it = DaprStateProtos.QueryStateItem.newBuilder().setKey(item.getKey());
    if (item.getValue() != null) {
      it.setData(ByteString.copyFrom(MAPPER.writeValueAsBytes(item.getValue())));
    }
    if (item.getEtag() != null) {
      it.setEtag(item.getEtag());
    }
    if (item.getError() != null) {
      it.setError(item.getError());
    }
    return it.build();
  }

  private static StatusRuntimeException newStatusRuntimeException(String status, String message) {
    return new StatusRuntimeException(Status.fromCode(Status.Code.valueOf(status)).withDescription(message));
  }

  // ==================== Encrypt Tests ====================

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when request is null")
  public void encryptNullRequestTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(null).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when component name is null")
  public void encryptNullComponentNameTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        null,
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when component name is empty")
  public void encryptEmptyComponentNameTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when component name is whitespace only")
  public void encryptWhitespaceComponentNameTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "   ",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when key name is null")
  public void encryptNullKeyNameTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        null,
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when key name is empty")
  public void encryptEmptyKeyNameTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "",
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when key wrap algorithm is null")
  public void encryptNullKeyWrapAlgorithmTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        null
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when key wrap algorithm is empty")
  public void encryptEmptyKeyWrapAlgorithmTest() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        ""
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should throw IllegalArgumentException when plaintext stream is null")
  public void encryptNullPlaintextStreamTest() {
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        null,
        "mykey",
        "RSA-OAEP-256"
    );

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.encrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("encrypt should successfully encrypt data with required fields")
  public void encryptSuccessTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] encryptedData = "encrypted-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      // Simulate returning encrypted data
      DaprCryptoProtos.EncryptResponse response = DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(encryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertArrayEquals(encryptedData, results.get(0));
  }

  @Test
  @DisplayName("encrypt should handle multiple response chunks")
  public void encryptMultipleChunksResponseTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] chunk1 = "chunk1".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "chunk2".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "chunk3".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      // Simulate returning multiple chunks
      responseObserver.onNext(DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk1))
              .setSeq(0)
              .build())
          .build());
      responseObserver.onNext(DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk2))
              .setSeq(1)
              .build())
          .build());
      responseObserver.onNext(DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk3))
              .setSeq(2)
              .build())
          .build());
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(3, results.size());
    assertArrayEquals(chunk1, results.get(0));
    assertArrayEquals(chunk2, results.get(1));
    assertArrayEquals(chunk3, results.get(2));
  }

  @Test
  @DisplayName("encrypt should handle optional data encryption cipher")
  public void encryptWithDataEncryptionCipherTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] encryptedData = "encrypted-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.EncryptResponse response = DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(encryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    ).setDataEncryptionCipher("aes-gcm");

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  @DisplayName("encrypt should handle omit decryption key name option")
  public void encryptWithOmitDecryptionKeyNameTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] encryptedData = "encrypted-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.EncryptResponse response = DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(encryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    ).setOmitDecryptionKeyName(true);

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  @DisplayName("encrypt should handle decryption key name option")
  public void encryptWithDecryptionKeyNameTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] encryptedData = "encrypted-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.EncryptResponse response = DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(encryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    ).setDecryptionKeyName("different-key");

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  @DisplayName("encrypt should handle all optional fields")
  public void encryptWithAllOptionalFieldsTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] encryptedData = "encrypted-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.EncryptResponse response = DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(encryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    )
    .setDataEncryptionCipher("chacha20-poly1305")
    .setOmitDecryptionKeyName(true)
    .setDecryptionKeyName("decrypt-key");

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  @DisplayName("encrypt should filter empty data from response")
  public void encryptFilterEmptyDataTest() {
    byte[] plaintext = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    byte[] validData = "valid-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.EncryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.EncryptResponse>) invocation.getArguments()[0];

      // Send empty data - should be filtered
      responseObserver.onNext(DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.EMPTY)
              .setSeq(0)
              .build())
          .build());
      
      // Send valid data
      responseObserver.onNext(DaprCryptoProtos.EncryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(validData))
              .setSeq(1)
              .build())
          .build());
      
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).encryptAlpha1(any());

    Flux<byte[]> plainTextStream = Flux.just(plaintext);
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent",
        plainTextStream,
        "mykey",
        "RSA-OAEP-256"
    );

    List<byte[]> results = previewClient.encrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertArrayEquals(validData, results.get(0));
  }

  // ==================== Decrypt Tests ====================

  @Test
  @DisplayName("decrypt should throw IllegalArgumentException when request is null")
  public void decryptNullRequestTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.decrypt(null).blockFirst();
    });
  }

  @Test
  @DisplayName("decrypt should throw IllegalArgumentException when component name is null")
  public void decryptNullComponentNameTest() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(null, cipherTextStream);

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.decrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("decrypt should throw IllegalArgumentException when component name is empty")
  public void decryptEmptyComponentNameTest() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("", cipherTextStream);

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.decrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("decrypt should throw IllegalArgumentException when component name is whitespace only")
  public void decryptWhitespaceComponentNameTest() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("   ", cipherTextStream);

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.decrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("decrypt should throw IllegalArgumentException when ciphertext stream is null")
  public void decryptNullCiphertextStreamTest() {
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", null);

    assertThrows(IllegalArgumentException.class, () -> {
      previewClient.decrypt(request).blockFirst();
    });
  }

  @Test
  @DisplayName("decrypt should successfully decrypt data with required fields")
  public void decryptSuccessTest() {
    byte[] ciphertext = "encrypted-data".getBytes(StandardCharsets.UTF_8);
    byte[] decryptedData = "Hello, World!".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.DecryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.DecryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.DecryptResponse response = DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(decryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).decryptAlpha1(any());

    Flux<byte[]> cipherTextStream = Flux.just(ciphertext);
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", cipherTextStream);

    List<byte[]> results = previewClient.decrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertArrayEquals(decryptedData, results.get(0));
  }

  @Test
  @DisplayName("decrypt should handle multiple response chunks")
  public void decryptMultipleChunksResponseTest() {
    byte[] ciphertext = "encrypted-data".getBytes(StandardCharsets.UTF_8);
    byte[] chunk1 = "chunk1".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "chunk2".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "chunk3".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.DecryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.DecryptResponse>) invocation.getArguments()[0];

      responseObserver.onNext(DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk1))
              .setSeq(0)
              .build())
          .build());
      responseObserver.onNext(DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk2))
              .setSeq(1)
              .build())
          .build());
      responseObserver.onNext(DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(chunk3))
              .setSeq(2)
              .build())
          .build());
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).decryptAlpha1(any());

    Flux<byte[]> cipherTextStream = Flux.just(ciphertext);
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", cipherTextStream);

    List<byte[]> results = previewClient.decrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(3, results.size());
    assertArrayEquals(chunk1, results.get(0));
    assertArrayEquals(chunk2, results.get(1));
    assertArrayEquals(chunk3, results.get(2));
  }

  @Test
  @DisplayName("decrypt should handle optional key name")
  public void decryptWithKeyNameTest() {
    byte[] ciphertext = "encrypted-data".getBytes(StandardCharsets.UTF_8);
    byte[] decryptedData = "Hello, World!".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.DecryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.DecryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.DecryptResponse response = DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(decryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).decryptAlpha1(any());

    Flux<byte[]> cipherTextStream = Flux.just(ciphertext);
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", cipherTextStream)
        .setKeyName("mykey");

    List<byte[]> results = previewClient.decrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertArrayEquals(decryptedData, results.get(0));
  }

  @Test
  @DisplayName("decrypt should filter empty data from response")
  public void decryptFilterEmptyDataTest() {
    byte[] ciphertext = "encrypted-data".getBytes(StandardCharsets.UTF_8);
    byte[] validData = "valid-data".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.DecryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.DecryptResponse>) invocation.getArguments()[0];

      // Send empty data - should be filtered
      responseObserver.onNext(DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.EMPTY)
              .setSeq(0)
              .build())
          .build());
      
      // Send valid data
      responseObserver.onNext(DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(validData))
              .setSeq(1)
              .build())
          .build());
      
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).decryptAlpha1(any());

    Flux<byte[]> cipherTextStream = Flux.just(ciphertext);
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", cipherTextStream);

    List<byte[]> results = previewClient.decrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertArrayEquals(validData, results.get(0));
  }

  @Test
  @DisplayName("decrypt should handle key name with version")
  public void decryptWithKeyNameVersionTest() {
    byte[] ciphertext = "encrypted-data".getBytes(StandardCharsets.UTF_8);
    byte[] decryptedData = "Hello, World!".getBytes(StandardCharsets.UTF_8);

    doAnswer((Answer<StreamObserver<DaprCryptoProtos.DecryptRequest>>) invocation -> {
      StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
          (StreamObserver<DaprCryptoProtos.DecryptResponse>) invocation.getArguments()[0];

      DaprCryptoProtos.DecryptResponse response = DaprCryptoProtos.DecryptResponse.newBuilder()
          .setPayload(CommonProtos.StreamPayload.newBuilder()
              .setData(ByteString.copyFrom(decryptedData))
              .setSeq(0)
              .build())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

      return mock(StreamObserver.class);
    }).when(daprStub).decryptAlpha1(any());

    Flux<byte[]> cipherTextStream = Flux.just(ciphertext);
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1("mycomponent", cipherTextStream)
        .setKeyName("mykey/v2");

    List<byte[]> results = previewClient.decrypt(request).collectList().block();

    assertNotNull(results);
    assertEquals(1, results.size());
  }
}
