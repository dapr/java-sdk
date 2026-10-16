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

import io.dapr.actors.ActorTrace;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseAlpha1;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import reactor.core.scheduler.Schedulers;

import java.io.Closeable;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hosts the actor types registered in {@link ActorRuntime} over the Dapr actor event stream
 * (SubscribeActorEventsAlpha1). Dapr delivers actor invocations, reminders, timers and deactivations on
 * this stream, so the app does not need to expose a server port for actor callbacks.
 *
 * <p>The stream reconnects automatically when it drops. It stops on errors that a retry cannot fix, such as
 * Dapr being configured with an HTTP app channel.
 *
 * <p>This API is in alpha and may change.
 */
public final class ActorEventSubscription implements Closeable {

  private static final String TRACE_TYPE = "ActorEventSubscription";

  private static final ActorTrace ACTOR_TRACE = new ActorTrace();

  private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

  private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

  /**
   * Dapr's default drain ongoing call timeout, used when the runtime config leaves it unset.
   */
  private static final Duration DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(2);

  private static final Set<Status.Code> TERMINAL_CODES = EnumSet.of(
      Status.Code.FAILED_PRECONDITION,
      Status.Code.UNIMPLEMENTED,
      Status.Code.INVALID_ARGUMENT,
      Status.Code.PERMISSION_DENIED,
      Status.Code.UNAUTHENTICATED);

  private final ActorRuntime runtime;

  private final DaprGrpc.DaprStub stub;

  private final ManagedChannel channel;

  private final Duration handshakeTimeout;

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final AtomicInteger inFlight = new AtomicInteger();

  private final CountDownLatch terminated = new CountDownLatch(1);

  private final Thread loop;

  private volatile Session session;

  private volatile Throwable terminalError;

  /**
   * Creates the subscription. Call {@link #start()} to open the stream.
   *
   * @param runtime          Actor runtime that handles the callbacks.
   * @param stub             Dapr gRPC stub.
   * @param channel          Channel behind the stub, used to reset its connect backoff before a retry (or null).
   * @param handshakeTimeout Maximum time to wait for Dapr to acknowledge the registration.
   */
  ActorEventSubscription(
      ActorRuntime runtime, DaprGrpc.DaprStub stub, ManagedChannel channel, Duration handshakeTimeout) {
    this.runtime = runtime;
    this.stub = stub;
    this.channel = channel;
    this.handshakeTimeout = handshakeTimeout;
    this.loop = new Thread(this::run, "dapr-actor-event-stream");
    this.loop.setDaemon(true);
  }

  void start() {
    this.loop.start();
  }

  /**
   * Returns whether the stream is open or reconnecting.
   *
   * @return True until the subscription is closed or stops on an error a retry cannot fix.
   */
  public boolean isRunning() {
    return running.get();
  }

  /**
   * Blocks until the subscription stops.
   *
   * @throws InterruptedException If the calling thread is interrupted.
   * @throws DaprException        If the stream stopped on an error a retry cannot fix.
   */
  public void awaitTermination() throws InterruptedException {
    terminated.await();
    if (terminalError != null) {
      throw new DaprException(terminalError);
    }
  }

  /**
   * Stops hosting actors. Waits for in-flight callbacks up to the drain ongoing call timeout, then closes the
   * stream.
   */
  @Override
  public void close() {
    running.set(false);

    Duration drainTimeout = runtime.getConfig().getDrainOngoingCallTimeout();
    if (drainTimeout == null) {
      drainTimeout = DEFAULT_DRAIN_TIMEOUT;
    }
    long deadline = System.nanoTime() + drainTimeout.toNanos();
    try {
      while (inFlight.get() > 0 && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Session current = session;
    if (current != null) {
      current.shutdown();
    }
    loop.interrupt();
    try {
      loop.join(TimeUnit.SECONDS.toMillis(5));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void run() {
    Duration backoff = INITIAL_BACKOFF;
    try {
      while (running.get()) {
        Session current = new Session();
        session = current;
        // close() sets running before reading session, so one of the two always sees the other.
        if (!running.get()) {
          return;
        }
        Throwable error = current.run();
        if (!running.get()) {
          return;
        }
        if (current.acknowledged) {
          backoff = INITIAL_BACKOFF;
        }

        Status.Code code = error == null ? Status.Code.OK : Status.fromThrowable(error).getCode();
        if (TERMINAL_CODES.contains(code)) {
          terminalError = error;
          ACTOR_TRACE.writeError(TRACE_TYPE, null, "Actor event stream stopped: %s", error.toString());
          return;
        }

        ACTOR_TRACE.writeWarning(TRACE_TYPE, null, "Actor event stream closed (%s), reconnecting in %s",
            error == null ? "completed by Dapr" : error.toString(), backoff);
        Thread.sleep(backoff.toMillis());
        backoff = backoff.multipliedBy(2).compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff.multipliedBy(2);
        if (channel != null) {
          channel.resetConnectBackoff();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      running.set(false);
      terminated.countDown();
    }
  }

  /**
   * One connection attempt. Replies from handlers that finish after the session closed are dropped, so they
   * never reach the stream of a later session.
   */
  private final class Session implements StreamObserver<SubscribeActorEventsResponseAlpha1> {

    private final CountDownLatch done = new CountDownLatch(1);

    private final CountDownLatch handshake = new CountDownLatch(1);

    private volatile boolean acknowledged;

    private volatile Throwable error;

    private ClientCallStreamObserver<SubscribeActorEventsRequestAlpha1> requests;

    private boolean closed;

    Throwable run() throws InterruptedException {
      synchronized (this) {
        if (closed) {
          return null;
        }
        requests = (ClientCallStreamObserver<SubscribeActorEventsRequestAlpha1>)
            stub.subscribeActorEventsAlpha1(this);
      }
      try {
        send(ActorEventDispatcher.buildInitialRequest(runtime.getConfig()));

        if (!handshake.await(handshakeTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
          cancel("timed out waiting for Dapr to acknowledge the actor registration");
        }
        done.await();
      } catch (InterruptedException e) {
        shutdown();
        throw e;
      }
      return error;
    }

    @Override
    public void onNext(SubscribeActorEventsResponseAlpha1 message) {
      if (!acknowledged) {
        if (message.getResponseTypeCase() != SubscribeActorEventsResponseAlpha1.ResponseTypeCase.INITIAL_RESPONSE) {
          cancel("expected the initial response, got " + message.getResponseTypeCase());
          return;
        }
        acknowledged = true;
        handshake.countDown();
        ACTOR_TRACE.writeInfo(TRACE_TYPE, null, "Hosting actor types %s over gRPC",
            runtime.getConfig().getRegisteredActorTypes());
        return;
      }

      inFlight.incrementAndGet();
      ActorEventDispatcher.dispatch(runtime, message)
          .subscribeOn(Schedulers.boundedElastic())
          .doFinally(signal -> inFlight.decrementAndGet())
          .subscribe(
              this::send,
              e -> ACTOR_TRACE.writeError(TRACE_TYPE, null, "Failed to handle actor event: %s", e.toString()));
    }

    @Override
    public void onError(Throwable t) {
      finish(t);
    }

    @Override
    public void onCompleted() {
      finish(null);
    }

    private void finish(Throwable t) {
      synchronized (this) {
        closed = true;
      }
      error = t;
      handshake.countDown();
      done.countDown();
    }

    /**
     * gRPC does not allow concurrent sends on a stream, so all writes go through this method.
     */
    private synchronized void send(SubscribeActorEventsRequestAlpha1 request) {
      if (closed) {
        return;
      }
      try {
        requests.onNext(request);
      } catch (RuntimeException e) {
        ACTOR_TRACE.writeWarning(TRACE_TYPE, null, "Failed to send actor event response: %s", e.toString());
      }
    }

    private synchronized void cancel(String reason) {
      if (closed) {
        return;
      }
      requests.cancel(reason, null);
    }

    private synchronized void shutdown() {
      if (closed) {
        return;
      }
      closed = true;
      if (requests != null) {
        requests.onCompleted();
        requests.cancel("actor event subscription closed", null);
      }
    }
  }
}
