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

import io.dapr.client.domain.CloudEvent;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprPubsubProtos;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Streaming subscription of events for Dapr's pubsub.
 * @param <T> Application's object type.
 */
@Deprecated
public class Subscription<T> implements Closeable {

  private final BlockingQueue<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> ackQueue =
      new LinkedBlockingQueue<>(50);

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final Semaphore receiverStateChange = new Semaphore(0);

  private Thread acker;

  private Thread receiver;

  Subscription(DaprGrpc.DaprStub asyncStub,
               DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request,
               SubscriptionListener<T> listener,
               Function<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1, CloudEvent<T>> cloudEventConverter) {
    final AtomicReference<StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1>> streamRef =
        new AtomicReference<>();

    this.acker = new Thread(() -> {
      while (running.get()) {
        try {
          var ackResponse = ackQueue.take();
          if (ackResponse == null) {
            continue;
          }

          var stream = streamRef.get();
          if (stream == null) {
            Thread.sleep(1000);
            // stream not ready yet
            continue;
          }

          stream.onNext(ackResponse);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        } catch (Exception e) {
          // No-op and continue after waiting for some time.
          // This is useful when there is a reconnection, for example.
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    });

    this.receiver = new Thread(() -> {
      while (running.get()) {
        var stream = asyncStub.subscribeTopicEventsAlpha1(new StreamObserver<>() {
          @Override
          public void onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 topicEventRequest) {
            try {
              var stream = streamRef.get();
              if (stream == null) {
                throw new RuntimeException("Cannot receive event: streaming subscription is not initialized.");
              }

              CloudEvent<T> cloudEvent = cloudEventConverter.apply(topicEventRequest);
              if (cloudEvent == null) {
                return;
              }

              var id = cloudEvent.getId();
              if ((id == null) || id.isEmpty()) {
                return;
              }

              onEvent(listener, cloudEvent).subscribe(status -> {
                var ack = buildAckRequest(id, status);
                try {
                  ackQueue.put(ack);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
            } catch (Exception e) {
              this.onError(DaprException.propagate(e));
            }
          }

          @Override
          public void onError(Throwable throwable) {
            listener.onError(DaprException.propagate(throwable));
          }

          @Override
          public void onCompleted() {
            receiverStateChange.release();
          }
        });

        streamRef.set(stream);
        stream.onNext(request);

        // Keep the client running
        try {
          receiverStateChange.acquire();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          running.set(false);
        }
      }
    });
  }

  private static <T> Mono<SubscriptionListener.Status> onEvent(
      SubscriptionListener<T> listener, CloudEvent<T> cloudEvent) {
    return listener.onEvent(cloudEvent).onErrorMap(t -> {
      var exception = DaprException.propagate(t);
      listener.onError(exception);
      return exception;
    }).onErrorReturn(SubscriptionListener.Status.RETRY);
  }

  @Nonnull
  private static DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildAckRequest(
      String id, SubscriptionListener.Status status) {
    DaprPubsubProtos.SubscribeTopicEventsRequestProcessedAlpha1 eventProcessed =
        DaprPubsubProtos.SubscribeTopicEventsRequestProcessedAlpha1.newBuilder()
            .setId(id)
            .setStatus(
                DaprAppCallbackProtos.TopicEventResponse.newBuilder()
                    .setStatus(DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.valueOf(
                        status.name()))
                    .build())
            .build();
    DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 ack =
        DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
            .setEventProcessed(eventProcessed)
            .build();
    return ack;
  }

  void start() {
    this.receiver.start();
    this.acker.start();
  }

  /**
   * Stops the subscription.
   */
  @Override
  public void close() {
    running.set(false);
    receiverStateChange.release();
    this.acker.interrupt();
  }

  /**
   * Awaits (blocks) for subscription to end.
   * @throws InterruptedException Exception if interrupted while awaiting.
   */
  public void awaitTermination() throws InterruptedException {
    this.receiver.join();
    this.acker.join();
  }
}
