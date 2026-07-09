/*
 * Copyright 2024 The Dapr Authors
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

import com.google.protobuf.ByteString;
import io.dapr.client.domain.CloudEvent;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprPubsubProtos;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class SubscriptionTest {

  private DaprGrpc.DaprStub asyncStub;
  private DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request;

  @BeforeEach
  public void setUp() {
    asyncStub = mock(DaprGrpc.DaprStub.class);
    request = DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder().build();
  }

  private static DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 event(String id) {
    return DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
        .setEventMessage(DaprAppCallbackProtos.TopicEventRequest.newBuilder()
            .setId(id)
            .setPubsubName("pubsub")
            .setTopic("topic")
            .setData(ByteString.copyFromUtf8("\"payload\""))
            .setDataContentType("application/json")
            .build())
        .build();
  }

  private static Function<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1, CloudEvent<String>>
      passthroughConverter() {
    return resp -> {
      var ce = new CloudEvent<String>();
      ce.setId(resp.getEventMessage().getId());
      ce.setPubsubName(resp.getEventMessage().getPubsubName());
      ce.setTopic(resp.getEventMessage().getTopic());
      ce.setData("payload");
      return ce;
    };
  }

  private static StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>
      noopRequestObserver() {
    return new StreamObserver<>() {
      @Override
      public void onNext(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 req) {
      }

      @Override
      public void onError(Throwable throwable) {
      }

      @Override
      public void onCompleted() {
      }
    };
  }

  /**
   * A per-event exception in onNext must notify the listener but keep the gRPC stream alive.
   * A single bad event should not tear down the whole subscription.
   */
  @Test
  @Timeout(15)
  public void perEventExceptionKeepsStreamAlive() throws Exception {
    AtomicInteger subscribeCalls = new AtomicInteger();
    AtomicReference<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>> observerRef =
        new AtomicReference<>();
    Semaphore subscribeCalled = new Semaphore(0);

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) inv -> {
      subscribeCalls.incrementAndGet();
      observerRef.set(inv.getArgument(0));
      subscribeCalled.release();
      return noopRequestObserver();
    }).when(asyncStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    Set<String> received = ConcurrentHashMap.newKeySet();
    List<String> errors = new CopyOnWriteArrayList<>();
    Semaphore gotGoodEvent = new Semaphore(0);

    SubscriptionListener<String> listener = new SubscriptionListener<>() {
      @Override
      public Mono<SubscriptionListener.Status> onEvent(CloudEvent<String> event) {
        received.add(event.getId());
        gotGoodEvent.release();
        return Mono.just(Status.SUCCESS);
      }

      @Override
      public void onError(RuntimeException exception) {
        errors.add(exception.getMessage());
      }
    };

    AtomicInteger convertCalls = new AtomicInteger();
    Function<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1, CloudEvent<String>> converter = resp -> {
      if (convertCalls.incrementAndGet() == 1) {
        throw new RuntimeException("boom");
      }
      return passthroughConverter().apply(resp);
    };

    var sub = new Subscription<>(asyncStub, request, listener, converter);
    sub.start();
    subscribeCalled.acquire();

    var observer = observerRef.get();
    observer.onNext(event("bad-" + UUID.randomUUID()));
    observer.onNext(event("good-" + UUID.randomUUID()));

    assertTrue(gotGoodEvent.tryAcquire(5, TimeUnit.SECONDS), "good event never delivered");

    // Give a moment to ensure no reconnect kicks off.
    Thread.sleep(200);
    sub.close();

    assertEquals(1, subscribeCalls.get(), "stream must not reconnect on per-event error");
    assertTrue(errors.stream().anyMatch(m -> m != null && m.contains("boom")),
        "listener.onError must be invoked with the propagated exception, got: " + errors);
    assertTrue(received.stream().anyMatch(id -> id.startsWith("good-")),
        "good event must still be delivered on same stream");
  }

  /**
   * Reconnect backoff must reset to 1s after a stream that had delivered at least one event
   * disconnects. Without the reset, an early disconnect during a healthy stream would inherit
   * the escalated backoff from earlier failures.
   */
  @Test
  @Timeout(30)
  public void backoffResetsAfterHealthyStreamDisconnect() throws Exception {
    LinkedBlockingQueue<Long> subscribeTimestamps = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>> observers =
        new LinkedBlockingQueue<>();
    Semaphore subscribeCalled = new Semaphore(0);

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) inv -> {
      subscribeTimestamps.add(System.nanoTime());
      observers.add(inv.getArgument(0));
      subscribeCalled.release();
      return noopRequestObserver();
    }).when(asyncStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    Semaphore eventProcessed = new Semaphore(0);
    SubscriptionListener<String> listener = new SubscriptionListener<>() {
      @Override
      public Mono<SubscriptionListener.Status> onEvent(CloudEvent<String> event) {
        eventProcessed.release();
        return Mono.just(Status.SUCCESS);
      }

      @Override
      public void onError(RuntimeException exception) {
      }
    };

    var sub = new Subscription<>(asyncStub, request, listener, passthroughConverter());
    sub.start();

    // Stream 1: no events, complete immediately. Backoff after: 1s -> 2s.
    subscribeCalled.acquire();
    observers.take().onCompleted();

    // Stream 2: no events, complete immediately. Backoff after: 2s -> 4s.
    subscribeCalled.acquire();
    observers.take().onCompleted();

    // Stream 3: deliver an event, then complete. Backoff must RESET to 1s.
    subscribeCalled.acquire();
    var third = observers.take();
    third.onNext(event("healthy-" + UUID.randomUUID()));
    assertTrue(eventProcessed.tryAcquire(5, TimeUnit.SECONDS));
    third.onCompleted();

    // Stream 4: capture the timestamp of the reconnect after the healthy stream.
    subscribeCalled.acquire();
    sub.close();

    List<Long> ts = List.copyOf(subscribeTimestamps);
    assertEquals(4, ts.size(), "expected 4 subscribeTopicEventsAlpha1 calls");

    long gap1to2Ms = TimeUnit.NANOSECONDS.toMillis(ts.get(1) - ts.get(0));
    long gap2to3Ms = TimeUnit.NANOSECONDS.toMillis(ts.get(2) - ts.get(1));
    long gap3to4Ms = TimeUnit.NANOSECONDS.toMillis(ts.get(3) - ts.get(2));

    // First reconnect: ~1s initial backoff.
    assertTrue(gap1to2Ms >= 800 && gap1to2Ms < 2500,
        "first reconnect gap should be ~1s, was " + gap1to2Ms + "ms");
    // Second reconnect: escalated to ~2s.
    assertTrue(gap2to3Ms >= 1800 && gap2to3Ms < 3500,
        "second reconnect gap should be ~2s (escalated), was " + gap2to3Ms + "ms");
    // After healthy stream: backoff RESET to ~1s. Without the reset it would be ~4s.
    assertTrue(gap3to4Ms < 2500,
        "reconnect gap after healthy stream must reset to ~1s (not escalated), was " + gap3to4Ms + "ms");
  }

  /**
   * close() must unblock the receiver even while it is sleeping in its reconnect backoff,
   * so shutdown is not delayed by up to 30s.
   */
  @Test
  @Timeout(10)
  public void closeInterruptsReconnectBackoff() throws Exception {
    Semaphore subscribeCalled = new Semaphore(0);
    LinkedBlockingQueue<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1>> observers =
        new LinkedBlockingQueue<>();

    doAnswer((Answer<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>>) inv -> {
      observers.add(inv.getArgument(0));
      subscribeCalled.release();
      return noopRequestObserver();
    }).when(asyncStub).subscribeTopicEventsAlpha1(any(StreamObserver.class));

    SubscriptionListener<String> listener = new SubscriptionListener<>() {
      @Override
      public Mono<SubscriptionListener.Status> onEvent(CloudEvent<String> event) {
        return Mono.just(Status.SUCCESS);
      }

      @Override
      public void onError(RuntimeException exception) {
      }
    };

    var sub = new Subscription<>(asyncStub, request, listener, passthroughConverter());
    sub.start();

    // First stream ends immediately; receiver enters Thread.sleep(backoffMs).
    subscribeCalled.acquire();
    observers.take().onCompleted();

    // Close mid-backoff. awaitTermination must return promptly, not after the full sleep.
    long start = System.nanoTime();
    sub.close();
    sub.awaitTermination();
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    assertTrue(elapsedMs < 2000, "close() should interrupt reconnect sleep quickly, took " + elapsedMs + "ms");
  }
}
