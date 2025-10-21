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
 * Common interface for task orchestrator implementations.
 *
 * <p>Task orchestrators describe how actions are executed and the order in which actions are executed. Orchestrators
 * don't call into external services or do complex computation directly. Rather, they delegate these tasks to
 * <em>activities</em>, which perform the actual work.</p>
 *
 * <p>Orchestrators can be scheduled using the {@link DurableTaskClient#scheduleNewOrchestrationInstance} method
 * overloads. Orchestrators can also invoke child orchestrators using the
 * {@link TaskOrchestrationContext#callSubOrchestrator} method overloads.</p>
 *
 * <p>Orchestrators may be replayed multiple times to rebuild their local state after being reloaded into memory.
 * Orchestrator code must therefore be <em>deterministic</em> to ensure no unexpected side effects from execution
 * replay. To account for this behavior, there are several coding constraints to be aware of:</p>
 * <ul>
 *     <li>
 *         An orchestrator must not generate random numbers or random UUIDs, get the current date, read environment
 *         variables, or do anything else that might result in a different value if the code is replayed in the future.
 *         Activities and built-in methods on the {@link TaskOrchestrationContext} parameter, like
 *         {@link TaskOrchestrationContext#getCurrentInstant()}, can be used to work around these restrictions.
 *     </li>
 *     <li>
 *         Orchestrator logic must be executed on the orchestrator thread. Creating new threads or scheduling callbacks
 *         onto background threads is forbidden and may result in failures or other unexpected behavior.
 *     </li>
 *     <li>
 *         Avoid infinite loops as they could cause the application to run out of memory. Instead, ensure that loops are
 *         bounded or use {@link TaskOrchestrationContext#continueAsNew} to restart an orchestrator with a new input.
 *     </li>
 *     <li>
 *         Avoid logging directly in the orchestrator code because log messages will be duplicated on each replay.
 *         Instead, check the value of the {@link TaskOrchestrationContext#getIsReplaying} method and write log messages
 *         only when it is {@code false}.
 *     </li>
 * </ul>
 *
 * <p>Orchestrator code is tightly coupled with its execution history so special care must be taken when making changes
 * to orchestrator code. For example, adding or removing activity tasks to an orchestrator's code may cause a
 * mismatch between code and history for in-flight orchestrations. To avoid potential issues related to orchestrator
 * versioning, consider applying the following strategies:</p>
 * <ul>
 *     <li>
 *         Deploy multiple versions of applications side-by-side allowing new code to run independently of old code.
 *     </li>
 *     <li>
 *         Rather than changing existing orchestrators, create new orchestrators that implement the modified behavior.
 *     </li>
 *     <li>
 *         Ensure all in-flight orchestrations are complete before applying code changes to existing orchestrator code.
 *     </li>
 *     <li>
 *         If possible, only make changes to orchestrator code that won't impact its history or execution path. For
 *         example, renaming variables or adding log statements have no impact on an orchestrator's execution path and
 *         are safe to apply to existing orchestrations.
 *     </li>
 * </ul>
 */
@FunctionalInterface
public interface TaskOrchestration {
  /**
   * Executes the orchestrator logic.
   *
   * @param ctx provides access to methods for scheduling durable tasks and getting information about the current
   *            orchestration instance.
   */
  void run(TaskOrchestrationContext ctx);
}
