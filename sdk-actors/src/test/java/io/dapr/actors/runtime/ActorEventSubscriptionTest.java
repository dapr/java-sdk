/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseInitialAlpha1;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActorEventSubscriptionTest {

  private static final SubscribeActorEventsResponseAlpha1 ACK = SubscribeActorEventsResponseAlpha1.newBuilder()
      .setInitialResponse(SubscribeActorEventsResponseInitialAlpha1.getDefaultInstance())
      .build();

  private FakeDapr dapr;

  private Server server;

  private ManagedChannel channel;

  private ActorRuntime runtime;

  private ActorEventSubscription subscription;

  @BeforeEach
  public void setup() throws Exception {
    dapr = new FakeDapr();
    String name = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(name).directExecutor().addService(dapr).build().start();
    channel = InProcessChannelBuilder.forName(name).directExecutor().build();

    runtime = ActorEventDispatcherTest.newRuntime();
    runtime.registerActor(ActorRuntimeTest.MyActorImpl.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (subscription != null) {
      subscription.close();
    }
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  public void registersThenAnswersEachRequestById() throws Exception {
    Stream stream = subscribe(Duration.ofSeconds(10));

    SubscribeActorEventsRequestAlpha1 initial = stream.next();
    assertEquals("MyGreatActor", initial.getInitialRequest().getEntities(0));
    stream.send(ACK);

    String actorId = "actor-1";
    stream.send(ActorEventDispatcherTest.invoke("a", "MyGreatActor", "say", actorId));
    assertEquals("a", stream.next().getInvokeResponse().getId());

    stream.send(ActorEventDispatcherTest.deactivate("b", "MyGreatActor", actorId));
    assertEquals("b", stream.next().getDeactivateResponse().getId());
  }

  @Test
  public void concurrentRequestsGetOneResponseEach() throws Exception {
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();
    stream.send(ACK);

    Set<String> expected = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      expected.add(String.valueOf(i));
      stream.send(ActorEventDispatcherTest.invoke(String.valueOf(i), "MyGreatActor", "say", "actor-" + i));
    }

    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      ids.add(stream.next().getInvokeResponse().getId());
    }
    assertEquals(expected, ids);
  }

  @Test
  public void reconnectsWhenDaprClosesTheStream() throws Exception {
    Stream first = subscribe(Duration.ofSeconds(10));
    first.next();
    first.send(ACK);

    first.responses.onCompleted();

    Stream second = dapr.nextStream();
    assertTrue(second.next().hasInitialRequest());
    assertTrue(subscription.isRunning());
  }

  @Test
  public void stopsWhenDaprRejectsTheStream() throws Exception {
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();

    stream.responses.onError(Status.FAILED_PRECONDITION
        .withDescription("actor event stream requires no HTTP app channel")
        .asRuntimeException());

    assertThrows(DaprException.class, () -> subscription.awaitTermination());
    assertFalse(subscription.isRunning());
    assertNull(dapr.streams.poll(1500, TimeUnit.MILLISECONDS));
  }

  @Test
  public void reconnectsWhenHandshakeTimesOut() throws Exception {
    Stream first = subscribe(Duration.ofMillis(200));
    first.next();

    Stream second = dapr.nextStream();
    assertTrue(second.next().hasInitialRequest());
  }

  @Test
  public void reconnectsWhenFirstMessageIsNotTheAck() throws Exception {
    Stream first = subscribe(Duration.ofSeconds(10));
    first.next();
    first.send(ActorEventDispatcherTest.invoke("a", "MyGreatActor", "say"));

    Stream second = dapr.nextStream();
    assertTrue(second.next().hasInitialRequest());
  }

  @Test
  public void closeStopsTheSubscription() throws Exception {
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();
    stream.send(ACK);

    subscription.close();

    subscription.awaitTermination();
    assertFalse(subscription.isRunning());
    assertTrue(stream.ended.poll(5, TimeUnit.SECONDS));
    assertNull(dapr.streams.poll(1500, TimeUnit.MILLISECONDS));
  }

  @Test
  public void closeRightAfterSubscribeLeavesNoStreamOpen() throws Exception {
    for (int i = 0; i < 50; i++) {
      subscription = runtime.subscribeActorEvents(DaprGrpc.newStub(channel), Duration.ofSeconds(10));
      subscription.close();

      subscription.awaitTermination();
      assertFalse(subscription.isRunning());
    }

    Stream stream;
    while ((stream = dapr.streams.poll()) != null) {
      assertTrue(stream.ended.poll(5, TimeUnit.SECONDS), "a stream was left open");
    }
  }

  @Test
  public void cannotSubscribeTwice() throws Exception {
    subscribe(Duration.ofSeconds(10));

    assertThrows(IllegalStateException.class,
        () -> runtime.subscribeActorEvents(DaprGrpc.newStub(channel), Duration.ofSeconds(10)));
  }

  @Test
  public void unexpectedMessageAfterAckIsIgnored() throws Exception {
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();
    stream.send(ACK);
    stream.send(ACK);

    stream.send(ActorEventDispatcherTest.invoke("a", "MyGreatActor", "say"));
    assertEquals("a", stream.next().getInvokeResponse().getId());
  }

  @Test
  public void closeWaitsForInFlightCallbacks() throws Exception {
    runtime.registerActor(SlowActorImpl.class);
    runtime.getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(5));
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();
    stream.send(ACK);

    stream.send(ActorEventDispatcherTest.invoke("slow", "SlowActor", "slow"));
    subscription.close();

    assertEquals("slow", stream.next().getInvokeResponse().getId());
  }

  @Test
  public void requestsReceivedWhileClosingAreRejected() throws Exception {
    runtime.registerActor(SlowActorImpl.class);
    runtime.getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(5));
    Stream stream = subscribe(Duration.ofSeconds(10));
    stream.next();
    stream.send(ACK);

    SlowActorImpl.STARTED.drainPermits();
    stream.send(ActorEventDispatcherTest.invoke("slow", "SlowActor", "slow"));
    assertTrue(SlowActorImpl.STARTED.tryAcquire(5, TimeUnit.SECONDS));
    Thread closer = new Thread(subscription::close);
    closer.start();
    while (subscription.isRunning()) {
      Thread.sleep(5);
    }

    stream.send(ActorEventDispatcherTest.invoke("late", "SlowActor", "slow"));
    SubscribeActorEventsRequestAlpha1 rejected = stream.next();
    assertEquals("late", rejected.getRequestFailed().getId());
    assertEquals(Status.Code.UNAVAILABLE.value(), rejected.getRequestFailed().getCode());
    assertEquals("slow", stream.next().getInvokeResponse().getId());
    closer.join(TimeUnit.SECONDS.toMillis(10));
  }

  @Test
  public void subscribeWithoutChannelFails() {
    assertThrows(IllegalStateException.class, () -> runtime.subscribeActorEvents());
  }

  @Test
  public void runtimeSubscribesOnItsChannelAndClosesTheSubscription() throws Exception {
    ActorRuntime channelRuntime = ActorEventDispatcherTest.newRuntime(channel);
    channelRuntime.registerActor(ActorRuntimeTest.MyActorImpl.class);
    subscription = channelRuntime.subscribeActorEvents();
    Stream first = dapr.nextStream();
    first.next();
    first.send(ACK);

    // Registered after subscribing: Dapr learns about it on the next reconnect.
    channelRuntime.registerActor(SlowActorImpl.class);
    first.responses.onCompleted();
    Stream second = dapr.nextStream();
    assertTrue(second.next().getInitialRequest().getEntitiesList().contains("SlowActor"));

    channelRuntime.close();
    assertFalse(subscription.isRunning());
    assertTrue(second.ended.poll(5, TimeUnit.SECONDS));
  }

  private Stream subscribe(Duration handshakeTimeout) throws InterruptedException {
    subscription = runtime.subscribeActorEvents(DaprGrpc.newStub(channel), handshakeTimeout);
    return dapr.nextStream();
  }

  public interface SlowActor {
    String slow();
  }

  @ActorType(name = "SlowActor")
  public static class SlowActorImpl extends AbstractActor implements SlowActor {

    static final Semaphore STARTED = new Semaphore(0);

    public SlowActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
      super(runtimeContext, id);
    }

    @Override
    public String slow() {
      STARTED.release();
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return "done";
    }
  }

  /**
   * One stream as seen by the fake Dapr server.
   */
  private static final class Stream {

    private final StreamObserver<SubscribeActorEventsResponseAlpha1> responses;

    private final BlockingQueue<SubscribeActorEventsRequestAlpha1> requests = new LinkedBlockingQueue<>();

    private final BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();

    private Stream(StreamObserver<SubscribeActorEventsResponseAlpha1> responses) {
      this.responses = responses;
    }

    private SubscribeActorEventsRequestAlpha1 next() throws InterruptedException {
      SubscribeActorEventsRequestAlpha1 request = requests.poll(5, TimeUnit.SECONDS);
      assertNotNull(request, "no message received from the app");
      return request;
    }

    private synchronized void send(SubscribeActorEventsResponseAlpha1 response) {
      responses.onNext(response);
    }
  }

  private static final class FakeDapr extends DaprGrpc.DaprImplBase {

    private final BlockingQueue<Stream> streams = new LinkedBlockingQueue<>();

    private Stream nextStream() throws InterruptedException {
      Stream stream = streams.poll(5, TimeUnit.SECONDS);
      assertNotNull(stream, "the app did not open a stream");
      return stream;
    }

    @Override
    public StreamObserver<SubscribeActorEventsRequestAlpha1> subscribeActorEventsAlpha1(
        StreamObserver<SubscribeActorEventsResponseAlpha1> responses) {
      Stream stream = new Stream(responses);
      streams.add(stream);
      return new StreamObserver<SubscribeActorEventsRequestAlpha1>() {
        @Override
        public void onNext(SubscribeActorEventsRequestAlpha1 request) {
          stream.requests.add(request);
        }

        @Override
        public void onError(Throwable t) {
          stream.ended.add(true);
        }

        @Override
        public void onCompleted() {
          stream.ended.add(true);
        }
      };
    }
  }
}
