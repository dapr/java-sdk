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

package io.dapr.durabletask.interruption;

import io.dapr.durabletask.TaskOrchestrationContext;

/**
 * Control flow {@code Throwable} class for orchestrator when invoke {@link TaskOrchestrationContext#continueAsNew}.
 * This {@code Throwable} must never be caught by user
 * code.
 *
 * <p>{@code ContinueAsNewInterruption} is thrown when an orchestrator calls
 * {@link TaskOrchestrationContext#continueAsNew}.
 * Catching {@code ContinueAsNewInterruption} in user code could prevent the orchestration from saving
 * state and scheduling new tasks, resulting in the orchestration getting stuck.</p>
 */
public class ContinueAsNewInterruption extends RuntimeException {
  public ContinueAsNewInterruption(String message) {
    super(message);
  }
}
