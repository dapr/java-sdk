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
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.query.Query;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());


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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());
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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

    Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(PUBSUB_NAME, TOPIC_NAME,
            "text/plain", Collections.singletonList("test"));
    BulkPublishResponse<String> res = result.block();
    Assertions.assertNotNull(res);
    assertEquals( 0, res.getFailedEntries().size(), "expected no entries in failed entries list");
  }

  @Test
  public void publishEventsWithRequestMetaTest() {
    doAnswer((Answer<BulkPublishResponse>) invocation -> {
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
      observer.onNext(builder.build());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
      DaprProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
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
      StreamObserver<DaprProtos.BulkPublishResponse> observer =
              (StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
      observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
      observer.onCompleted();
      return null;
    }).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
      DaprProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
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
    DaprProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
    doAnswer(invocation -> {
      DaprProtos.QueryStateRequest req = (DaprProtos.QueryStateRequest) invocation.getArgument(0);
      assertEquals(QUERY_STORE_NAME, req.getStoreName());
      assertEquals("query", req.getQuery());
      assertEquals(0, req.getMetadataCount());

      StreamObserver<DaprProtos.QueryStateResponse> observer = (StreamObserver<DaprProtos.QueryStateResponse>)
              invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).queryStateAlpha1(any(DaprProtos.QueryStateRequest.class), any());

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
    DaprProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
    doAnswer(invocation -> {
      DaprProtos.QueryStateRequest req = (DaprProtos.QueryStateRequest) invocation.getArgument(0);
      assertEquals(QUERY_STORE_NAME, req.getStoreName());
      assertEquals("query", req.getQuery());
      assertEquals(1, req.getMetadataCount());
      assertEquals(1, req.getMetadataCount());

      StreamObserver<DaprProtos.QueryStateResponse> observer = (StreamObserver<DaprProtos.QueryStateResponse>)
              invocation.getArguments()[1];
      observer.onNext(responseEnvelope);
      observer.onCompleted();
      return null;
    }).when(daprStub).queryStateAlpha1(any(DaprProtos.QueryStateRequest.class), any());

    QueryStateResponse<String> response = previewClient.queryState(QUERY_STORE_NAME, "query",
            new HashMap<String, String>(){{ put("key", "error"); }}, String.class).block();
    assertNotNull(response);
    assertEquals(1, response.getResults().size(), "result size must be 1");
    assertEquals( "1", response.getResults().get(0).getKey(), "result must be same");
    assertEquals( "error data", response.getResults().get(0).getError(), "result must be same");
  }

  @Test
  public void tryLock() {

    DaprProtos.TryLockResponse.Builder builder = DaprProtos.TryLockResponse.newBuilder()
            .setSuccess(true);

    DaprProtos.TryLockResponse response = builder.build();

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.TryLockRequest req = invocation.getArgument(0);
      assertEquals(LOCK_STORE_NAME, req.getStoreName());
      assertEquals("1", req.getResourceId());
      assertEquals("owner", req.getLockOwner());
      assertEquals(10, req.getExpiryInSeconds());

      StreamObserver<DaprProtos.TryLockResponse> observer =
              (StreamObserver<DaprProtos.TryLockResponse>) invocation.getArguments()[1];
      observer.onNext(response);
      observer.onCompleted();
      return null;
    }).when(daprStub).tryLockAlpha1(any(DaprProtos.TryLockRequest.class), any());

    Boolean result = previewClient.tryLock("MyLockStore", "1", "owner", 10).block();
    assertEquals(Boolean.TRUE, result);
  }

  @Test
  public void unLock() {

    DaprProtos.UnlockResponse.Builder builder = DaprProtos.UnlockResponse.newBuilder()
            .setStatus(DaprProtos.UnlockResponse.Status.SUCCESS);

    DaprProtos.UnlockResponse response = builder.build();

    doAnswer((Answer<Void>) invocation -> {
      DaprProtos.UnlockRequest req = invocation.getArgument(0);
      assertEquals(LOCK_STORE_NAME, req.getStoreName());
      assertEquals("1", req.getResourceId());
      assertEquals("owner", req.getLockOwner());

      StreamObserver<DaprProtos.UnlockResponse> observer =
              (StreamObserver<DaprProtos.UnlockResponse>) invocation.getArguments()[1];
      observer.onNext(response);
      observer.onCompleted();
      return null;
    }).when(daprStub).unlockAlpha1(any(DaprProtos.UnlockRequest.class), any());

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

    doAnswer((Answer<StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1>>) invocation -> {
      StreamObserver<DaprProtos.SubscribeTopicEventsResponseAlpha1> observer =
              (StreamObserver<DaprProtos.SubscribeTopicEventsResponseAlpha1>) invocation.getArguments()[0];
      var emitterThread = new Thread(() -> {
        try {
          started.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        observer.onNext(DaprProtos.SubscribeTopicEventsResponseAlpha1.getDefaultInstance());
        for (int i = 0; i < numEvents; i++) {
          observer.onNext(DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
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
          observer.onNext(DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
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
        public void onNext(DaprProtos.SubscribeTopicEventsRequestAlpha1 subscribeTopicEventsRequestAlpha1) {
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
    DaprProtos.ConversationResponse conversationResponse = DaprProtos.ConversationResponse.newBuilder()
            .addOutputs(DaprProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprProtos.ConversationRequest.class), any());

    List<ConversationInput> inputs = new ArrayList<>();
    inputs.add(new ConversationInput("Hello there"));
    ConversationResponse response =
            previewClient.converse(new ConversationRequest("openai", inputs)).block();

    ArgumentCaptor<DaprProtos.ConversationRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ConversationRequest.class);
    verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

    DaprProtos.ConversationRequest conversationRequest = captor.getValue();

    assertEquals("openai", conversationRequest.getName());
    assertEquals("Hello there", conversationRequest.getInputs(0).getContent());
    assertEquals("Hello How are you",
            response.getConversationOutputs().get(0).getResult());
  }

  @Test
  public void converseShouldReturnConversationResponseWhenRequiredAndOptionalInputsAreValid() throws Exception {
    DaprProtos.ConversationResponse conversationResponse = DaprProtos.ConversationResponse.newBuilder()
            .setContextID("contextId")
            .addOutputs(DaprProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprProtos.ConversationRequest.class), any());

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

    ArgumentCaptor<DaprProtos.ConversationRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ConversationRequest.class);
    verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

    DaprProtos.ConversationRequest conversationRequest = captor.getValue();

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
  public void scheduleJobShouldSucceedWhenAllFieldsArePresentInRequest() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    ScheduleJobRequest expectedScheduleJobRequest = new ScheduleJobRequest("testJob",
            JobSchedule.fromString("*/5 * * * *"))
            .setData("testData".getBytes())
            .setTtl(Instant.now().plus(1, ChronoUnit.DAYS))
            .setRepeat(5)
            .setDueTime(Instant.now().plus(10, ChronoUnit.MINUTES));

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    assertDoesNotThrow(() -> previewClient.scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobReq = captor.getValue();

    assertEquals("testJob", actualScheduleJobReq.getJob().getName());
    assertEquals("testData",
            new String(actualScheduleJobReq.getJob().getData().getValue().toByteArray(), StandardCharsets.UTF_8));
    assertEquals("*/5 * * * *", actualScheduleJobReq.getJob().getSchedule());
    assertEquals(iso8601Formatter.format(expectedScheduleJobRequest.getTtl()), actualScheduleJobReq.getJob().getTtl());
    assertEquals(expectedScheduleJobRequest.getRepeats(), actualScheduleJobReq.getJob().getRepeats());
    assertEquals(iso8601Formatter.format(expectedScheduleJobRequest.getDueTime()), actualScheduleJobReq.getJob().getDueTime());
  }

  @Test
  public void scheduleJobShouldSucceedWhenRequiredFieldsNameAndDueTimeArePresentInRequest() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    ScheduleJobRequest expectedScheduleJobRequest =
            new ScheduleJobRequest("testJob", Instant.now().plus(10, ChronoUnit.MINUTES));
    assertDoesNotThrow(() -> previewClient.scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobRequest = captor.getValue();
    DaprProtos.Job job = actualScheduleJobRequest.getJob();
    assertEquals("testJob", job.getName());
    assertFalse(job.hasData());
    assertFalse(job.hasSchedule());
    assertEquals(0, job.getRepeats());
    assertFalse(job.hasTtl());
    assertEquals(iso8601Formatter.format(expectedScheduleJobRequest.getDueTime()),
            actualScheduleJobRequest.getJob().getDueTime());
  }

  @Test
  public void scheduleJobShouldSucceedWhenRequiredFieldsNameAndScheduleArePresentInRequest() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    ScheduleJobRequest expectedScheduleJobRequest = new ScheduleJobRequest("testJob",
            JobSchedule.fromString("* * * * * *"));
    assertDoesNotThrow(() -> previewClient.scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobRequest = captor.getValue();
    DaprProtos.Job job = actualScheduleJobRequest.getJob();
    assertEquals("testJob", job.getName());
    assertFalse(job.hasData());
    assertEquals( "* * * * * *", job.getSchedule());
    assertEquals(0, job.getRepeats());
    assertFalse(job.hasTtl());
  }

  @Test
  public void scheduleJobShouldThrowWhenRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.scheduleJob(null).block();
    });
    assertEquals("scheduleJobRequest cannot be null", exception.getMessage());
  }

  @Test
  public void scheduleJobShouldThrowWhenInvalidRequest() {
    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest(null, Instant.now());
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.scheduleJob(scheduleJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  public void scheduleJobShouldThrowWhenNameInRequestIsEmpty() {
    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("", Instant.now());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.scheduleJob(scheduleJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  public void getJobShouldReturnResponseWhenAllFieldsArePresentInRequest() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    GetJobRequest getJobRequest = new GetJobRequest("testJob");

    DaprProtos.Job job = DaprProtos.Job.newBuilder()
            .setName("testJob")
            .setTtl(OffsetDateTime.now().format(iso8601Formatter))
            .setData(Any.newBuilder().setValue(ByteString.copyFrom("testData".getBytes())).build())
            .setSchedule("*/5 * * * *")
            .setRepeats(5)
            .setDueTime(iso8601Formatter.format(Instant.now().plus(10, ChronoUnit.MINUTES)))
            .build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetJobResponse> observer = invocation.getArgument(1);
      observer.onNext(DaprProtos.GetJobResponse.newBuilder()
              .setJob(job)
              .build());
      observer.onCompleted();
      return null;
    }).when(daprStub).getJobAlpha1(any(DaprProtos.GetJobRequest.class), any());

    Mono<GetJobResponse> resultMono = previewClient.getJob(getJobRequest);

    GetJobResponse response = resultMono.block();
    assertNotNull(response);
    assertEquals("testJob", response.getName());
    assertEquals("testData", new String(response.getData(), StandardCharsets.UTF_8));
    assertEquals("*/5 * * * *", response.getSchedule().getExpression());
    assertEquals(5, response.getRepeats());
    assertEquals(job.getTtl(), iso8601Formatter.format(response.getTtl()));
    assertEquals(job.getDueTime(), iso8601Formatter.format(response.getDueTime()));
  }

  @Test
  public void getJobShouldReturnResponseWithScheduleSetWhenResponseHasSchedule() {
    GetJobRequest getJobRequest = new GetJobRequest("testJob");

    DaprProtos.Job job = DaprProtos.Job.newBuilder()
            .setName("testJob")
            .setSchedule("0 0 0 1 1 *")
            .build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetJobResponse> observer = invocation.getArgument(1);
      observer.onNext(DaprProtos.GetJobResponse.newBuilder()
              .setJob(job)
              .build());
      observer.onCompleted();
      return null;
    }).when(daprStub).getJobAlpha1(any(DaprProtos.GetJobRequest.class), any());

    Mono<GetJobResponse> resultMono = previewClient.getJob(getJobRequest);

    GetJobResponse response = resultMono.block();
    assertNotNull(response);
    assertEquals("testJob", response.getName());
    assertNull(response.getData());
    assertEquals("0 0 0 1 1 *", response.getSchedule().getExpression());
    assertNull(response.getRepeats());
    assertNull(response.getTtl());
    assertNull(response.getDueTime());
  }

  @Test
  public void getJobShouldReturnResponseWithDueTimeSetWhenResponseHasDueTime() {
    GetJobRequest getJobRequest = new GetJobRequest("testJob");

    String datetime = OffsetDateTime.now().toString();
    DaprProtos.Job job = DaprProtos.Job.newBuilder()
            .setName("testJob")
            .setDueTime(datetime)
            .build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetJobResponse> observer = invocation.getArgument(1);
      observer.onNext(DaprProtos.GetJobResponse.newBuilder()
              .setJob(job)
              .build());
      observer.onCompleted();
      return null;
    }).when(daprStub).getJobAlpha1(any(DaprProtos.GetJobRequest.class), any());

    Mono<GetJobResponse> resultMono = previewClient.getJob(getJobRequest);

    GetJobResponse response = resultMono.block();
    assertNotNull(response);
    assertEquals("testJob", response.getName());
    assertNull(response.getData());
    assertNull(response.getSchedule());
    assertNull(response.getRepeats());
    assertNull(response.getTtl());
    assertEquals(job.getDueTime(), datetime);
  }

  @Test
  public void getJobShouldThrowWhenRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.getJob(null).block();
    });
    assertEquals("getJobRequest cannot be null", exception.getMessage());
  }

  @Test
  public void getJobShouldThrowWhenNameIsNullRequest() {
    GetJobRequest getJobRequest = new GetJobRequest(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.getJob(getJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  public void getJobShouldThrowWhenNameIsEmptyRequest() {
    GetJobRequest getJobRequest =new GetJobRequest("");;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.getJob(getJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  public void deleteJobShouldSucceedWhenValidRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest("testJob");

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.DeleteJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).deleteJobAlpha1(any(DaprProtos.DeleteJobRequest.class), any());

    Mono<Void> resultMono = previewClient.deleteJob(deleteJobRequest);

    assertDoesNotThrow(() -> resultMono.block());
  }

  @Test
  public void deleteJobShouldThrowRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.deleteJob(null).block();
    });
    assertEquals("deleteJobRequest cannot be null", exception.getMessage());
  }

  @Test
  public void deleteJobShouldThrowWhenNameIsNullRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest(null);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.deleteJob(deleteJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  public void deleteJobShouldThrowWhenNameIsEmptyRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest("");
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      previewClient.deleteJob(deleteJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  private DaprProtos.QueryStateResponse buildQueryStateResponse(List<QueryStateItem<?>> resp,String token)
          throws JsonProcessingException {
    List<DaprProtos.QueryStateItem> items = new ArrayList<>();
    for (QueryStateItem<?> item: resp) {
      items.add(buildQueryStateItem(item));
    }
    return DaprProtos.QueryStateResponse.newBuilder()
            .addAllResults(items)
            .setToken(token)
            .build();
  }

  private DaprProtos.QueryStateItem buildQueryStateItem(QueryStateItem<?> item) throws JsonProcessingException {
    DaprProtos.QueryStateItem.Builder it = DaprProtos.QueryStateItem.newBuilder().setKey(item.getKey());
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
}
