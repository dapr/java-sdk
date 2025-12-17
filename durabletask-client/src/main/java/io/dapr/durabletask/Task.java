/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask;

import io.dapr.durabletask.interruption.OrchestratorBlockedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an asynchronous operation in a durable orchestration.
 *
 * <p>{@code Task<V>} instances are created by methods on the {@link TaskOrchestrationContext} class, which is available
 * in {@link TaskOrchestration} implementations. For example, scheduling an activity will return a task.</p>
 * <pre>
 * Task{@literal <}int{@literal >} activityTask = ctx.callActivity("MyActivity", someInput, int.class);
 * </pre>
 * <p>Orchestrator code uses the {@link #await()} method to block on the completion of the task and retrieve the result.
 * If the task is not yet complete, the {@code await()} method will throw an {@link OrchestratorBlockedException}, which
 * pauses the orchestrator's execution so that it can save its progress into durable storage and schedule any
 * outstanding work. When the task is complete, the orchestrator will run again from the beginning and the next time
 * the task's {@code await()} method is called, the result will be returned, or a {@link TaskFailedException} will be
 * thrown if the result of the task was an unhandled exception.</p>
 * <p>Note that orchestrator code must never catch {@code OrchestratorBlockedException} because doing so can cause the
 * orchestration instance to get permanently stuck.</p>
 *
 * @param <V> the return type of the task
 */
public abstract class Task<V> {
  final CompletableFuture<V> future;

  Task(CompletableFuture<V> future) {
    this.future = future;
  }

  /**
   * Returns {@code true} if completed in any fashion: normally, with an exception, or via cancellation.
   *
   * @return {@code true} if completed, otherwise {@code false}
   */
  public boolean isDone() {
    return this.future.isDone();
  }

  /**
   * Returns {@code true} if the task was cancelled.
   *
   * @return {@code true} if the task was cancelled, otherwise {@code false}
   */
  public boolean isCancelled() {
    return this.future.isCancelled();
  }

  /**
   * Blocks the orchestrator until this task to complete, and then returns its result.
   *
   * @return the result of the task
   */
  public abstract V await();

  /**
   * Returns a new {@link Task} that, when this Task completes normally,
   * is executed with this Task's result as the argument to the supplied function.
   *
   * @param fn  the function to use to compute the value of the returned Task
   * @param <U> the function's return type
   * @return the new Task
   */
  public abstract <U> Task<U> thenApply(Function<V, U> fn);

  /**
   * Returns a new {@link Task} that, when this Task completes normally,
   * is executed with this Task's result as the argument to the supplied action.
   *
   * @param fn the function to use to compute the value of the returned Task
   * @return the new Task
   */
  public abstract Task<Void> thenAccept(Consumer<V> fn);
}
