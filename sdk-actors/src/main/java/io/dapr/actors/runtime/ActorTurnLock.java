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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runs at most one turn at a time per actor, in arrival order.
 *
 * <p>Dapr can stop waiting before the actor code finishes, for example when the caller cancels, and then sends the
 * next call for the same actor. A turn therefore keeps the actor until its code has finished, not until its request
 * is cancelled.
 *
 * <p>There is no timeout: a turn that never finishes keeps its actor, and every later call for that actor waits.
 * Reentrancy is not supported, so a call that needs the actor's own turn to finish first waits forever.
 */
final class ActorTurnLock {

  /**
   * Turns per actor, in arrival order. The first one is running.
   */
  private final Map<ActorId, Deque<Turn<?>>> turns = new HashMap<>();

  /**
   * Runs a turn once the previous turns for the same actor have finished.
   *
   * <p>Cancelling the returned Mono while the turn waits removes it. Once the turn is next in line or has started,
   * cancelling only stops waiting for its result: the turn still runs to the end and keeps the actor until then.
   *
   * <p>The turn's work sees the Reactor context of the subscriber that queued it.
   *
   * @param actorId Actor identifier.
   * @param body    Supplies the turn's work. Called when the turn starts.
   * @param <T>     Result type.
   * @return Asynchronous result of the turn.
   */
  <T> Mono<T> run(ActorId actorId, Supplier<Mono<T>> body) {
    return Mono.deferContextual(context -> {
      Turn<T> turn = new Turn<>(actorId, body, context);
      boolean first;
      synchronized (this) {
        Deque<Turn<?>> queue = this.turns.computeIfAbsent(actorId, id -> new ArrayDeque<>());
        queue.addLast(turn);
        first = queue.size() == 1;
      }

      if (first) {
        turn.start(false);
      }

      return Mono.fromFuture(turn.result, true).doOnCancel(() -> this.withdraw(turn));
    });
  }

  /**
   * Removes a turn that has not started. A started turn is left to finish.
   */
  private synchronized void withdraw(Turn<?> turn) {
    Deque<Turn<?>> queue = this.turns.get(turn.actorId);
    if (queue != null && queue.peekFirst() != turn) {
      queue.remove(turn);
    }
  }

  /**
   * Ends the running turn and starts the next one, if any.
   */
  private void finish(Turn<?> turn) {
    Turn<?> next;
    synchronized (this) {
      Deque<Turn<?>> queue = this.turns.get(turn.actorId);
      queue.removeFirst();
      next = queue.peekFirst();
      if (next == null) {
        this.turns.remove(turn.actorId);
      }
    }

    if (next != null) {
      next.start(true);
    }
  }

  private final class Turn<T> {

    private final ActorId actorId;

    private final Supplier<Mono<T>> body;

    private final ContextView context;

    private final CompletableFuture<T> result = new CompletableFuture<>();

    private T value;

    private Turn(ActorId actorId, Supplier<Mono<T>> body, ContextView context) {
      this.actorId = actorId;
      this.body = body;
      this.context = context;
    }

    /**
     * Starts the turn. A turn handed over by the previous one runs on another thread, so a long queue of turns
     * that finish synchronously does not grow the stack.
     */
    private void start(boolean handedOver) {
      Mono<T> work = Mono.defer(this.body);
      if (handedOver) {
        work = work.subscribeOn(Schedulers.boundedElastic());
      }

      work.contextWrite(this.context).subscribe(
          v -> this.value = v,
          e -> {
            finish(this);
            this.result.completeExceptionally(e);
          },
          () -> {
            finish(this);
            this.result.complete(this.value);
          });
    }
  }
}
