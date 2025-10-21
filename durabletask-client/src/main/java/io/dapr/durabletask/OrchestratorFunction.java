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

/**
 * Functional interface for inline orchestrator functions.
 *
 * <p>See the description of {@link TaskOrchestration} for more information about how to correctly
 * implement orchestrators.</p>
 *
 * @param <R> the type of the result returned by the function
 */
@FunctionalInterface
public interface OrchestratorFunction<R> {
  /**
   * Executes an orchestrator function and returns a result to use as the orchestration output.
   *
   * <p>This functional interface is designed to support implementing orchestrators as lambda functions. It's intended
   * to be very similar to {@link java.util.function.Function}, but with a signature that's specific to
   * orchestrators.</p>
   *
   * @param ctx the orchestration context, which provides access to additional context for the current orchestration
   *            execution
   * @return the serializable output of the orchestrator function
   */
  R apply(TaskOrchestrationContext ctx);
}
