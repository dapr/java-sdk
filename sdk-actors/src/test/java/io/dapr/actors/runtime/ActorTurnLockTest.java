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
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActorTurnLockTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final ActorTurnLock lock = new ActorTurnLock();

  @Test
  public void runsTurnsForSameActorOneAtATime() {
    ActorId actorId = new ActorId("1");
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maxRunning = new AtomicInteger();

    Mono<Void> turns = Mono.when(
        lock.run(actorId, () -> track(running, maxRunning)),
        lock.run(actorId, () -> track(running, maxRunning)),
        lock.run(actorId, () -> track(running, maxRunning)));

    turns.block(TIMEOUT);
    assertEquals(1, maxRunning.get());
  }

  @Test
  public void runsTurnsForDifferentActorsAtTheSameTime() {
    Sinks.Empty<Void> first = Sinks.empty();
    Mono<Void> firstTurn = lock.run(new ActorId("1"), first::asMono);
    Disposable firstSubscription = firstTurn.subscribe();

    assertEquals("done", lock.run(new ActorId("2"), () -> Mono.just("done")).block(TIMEOUT));

    first.tryEmitEmpty();
    firstSubscription.dispose();
  }

  @Test
  public void runsTurnsInArrivalOrder() {
    ActorId actorId = new ActorId("1");
    Sinks.Empty<Void> first = Sinks.empty();
    List<Integer> order = new CopyOnWriteArrayList<>();

    Mono<Void> turns = Mono.when(
        lock.run(actorId, () -> first.asMono().doOnTerminate(() -> order.add(1))),
        lock.run(actorId, () -> Mono.fromRunnable(() -> order.add(2))),
        lock.run(actorId, () -> Mono.fromRunnable(() -> order.add(3))));
    Disposable subscription = turns.subscribe();
    first.tryEmitEmpty();

    waitFor(() -> order.size() == 3);
    assertEquals(List.of(1, 2, 3), order);
    subscription.dispose();
  }

  @Test
  public void cancelledRunningTurnKeepsActorUntilItFinishes() throws Exception {
    ActorId actorId = new ActorId("1");
    Sinks.Empty<Void> first = Sinks.empty();
    AtomicInteger firstFinished = new AtomicInteger();
    CountDownLatch firstStarted = new CountDownLatch(1);

    Disposable firstCaller = lock.run(actorId, () -> {
      firstStarted.countDown();
      return first.asMono().doOnSuccess(v -> firstFinished.incrementAndGet());
    }).subscribe();
    assertTrue(firstStarted.await(5, TimeUnit.SECONDS));

    // The caller of the running turn goes away, as when Dapr stops waiting for it.
    firstCaller.dispose();

    CompletableFuture<Integer> second = lock.run(actorId, () -> Mono.fromSupplier(firstFinished::get)).toFuture();
    Thread.sleep(100);
    assertFalse(second.isDone());

    first.tryEmitEmpty();
    // The second turn saw the first one finished.
    assertEquals(1, second.get(5, TimeUnit.SECONDS));
  }

  @Test
  public void cancelledWaitingTurnNeverRuns() {
    ActorId actorId = new ActorId("1");
    Sinks.Empty<Void> first = Sinks.empty();
    AtomicInteger secondRuns = new AtomicInteger();

    Disposable firstCaller = lock.run(actorId, first::asMono).subscribe();
    Disposable secondCaller = lock.run(actorId, () -> Mono.fromRunnable(secondRuns::incrementAndGet)).subscribe();
    secondCaller.dispose();
    first.tryEmitEmpty();

    assertEquals("third", lock.run(actorId, () -> Mono.just("third")).block(TIMEOUT));
    assertEquals(0, secondRuns.get());
    firstCaller.dispose();
  }

  @Test
  public void failedTurnReleasesActor() {
    ActorId actorId = new ActorId("1");

    // A type of its own, so a timeout from block() cannot pass for the turn's error.
    assertThrows(TurnFailure.class,
        () -> lock.run(actorId, () -> Mono.error(new TurnFailure())).block(TIMEOUT));
    assertThrows(TurnFailure.class, () -> lock.run(actorId, () -> {
      throw new TurnFailure();
    }).block(TIMEOUT));
    assertEquals("next", lock.run(actorId, () -> Mono.just("next")).block(TIMEOUT));
  }

  @Test
  public void emptyTurnReleasesActor() {
    ActorId actorId = new ActorId("1");

    assertNull(lock.run(actorId, Mono::<String>empty).block(TIMEOUT));
    assertEquals("next", lock.run(actorId, () -> Mono.just("next")).block(TIMEOUT));
  }

  @Test
  public void turnDoesNotStartUntilSubscribed() {
    AtomicInteger runs = new AtomicInteger();

    lock.run(new ActorId("1"), () -> Mono.fromRunnable(runs::incrementAndGet));

    assertEquals(0, runs.get());
  }

  @Test
  public void runsLongQueueOfSynchronousTurns() {
    ActorId actorId = new ActorId("1");
    Sinks.Empty<Void> first = Sinks.empty();
    AtomicInteger runs = new AtomicInteger();

    Disposable firstCaller = lock.run(actorId, first::asMono).subscribe();
    Mono<Void> turns = Mono.when(IntStream.range(0, 10_000)
        .mapToObj(i -> lock.run(actorId, () -> Mono.fromRunnable(runs::incrementAndGet)))
        .toArray(Mono[]::new));
    Disposable subscription = turns.subscribe();
    first.tryEmitEmpty();

    waitFor(() -> runs.get() == 10_000);
    subscription.dispose();
    firstCaller.dispose();
  }

  @Test
  public void turnSeesCallerContext() {
    ActorId actorId = new ActorId("1");
    Sinks.Empty<Void> first = Sinks.empty();
    Mono<String> readContext = Mono.deferContextual(context -> Mono.just(context.get("caller")));

    CompletableFuture<String> firstTurn = lock.run(actorId, () -> first.asMono().then(readContext))
        .contextWrite(Context.of("caller", "first"))
        .toFuture();
    // Queued behind the first turn, so it is started by the lock rather than by its own subscriber.
    CompletableFuture<String> secondTurn = lock.run(actorId, () -> readContext)
        .contextWrite(Context.of("caller", "second"))
        .toFuture();
    first.tryEmitEmpty();

    assertEquals("first", firstTurn.join());
    assertEquals("second", secondTurn.join());
  }

  private static class TurnFailure extends RuntimeException {
  }

  private static Mono<Void> track(AtomicInteger running, AtomicInteger maxRunning) {
    return Mono.fromRunnable(() -> maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max))
        .then(Mono.delay(Duration.ofMillis(50)))
        .doOnTerminate(running::decrementAndGet)
        .then();
  }

  private static void waitFor(BooleanSupplier condition) {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("timed out waiting for condition");
      }
      Thread.onSpinWait();
    }
  }
}
