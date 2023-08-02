/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.runtime;

import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Context object used by workflow implementations to perform actions such as scheduling activities,
 * durable timers, waiting for external events, and for getting basic information about the current
 * workflow instance.
 */
public interface WorkflowContext {

  /**
   * Get a logger only when {@code isReplaying} is false.
   * Otherwise, return a NOP (no operation) logger.
   *
   * @return Logger
   */
  Logger getLogger();


  /**
   * Gets the name of the current workflow.
   *
   * @return Asynchronous result with the name of the current workflow
   */
  Mono<String> getName();

  /**
   * Gets the instance ID of the current workflow.
   *
   * @return Asynchronous result with the instance ID of the current workflow
   */
  Mono<String> getInstanceId();

  /**
   * Completes the current workflow.
   *
   * @param output the serializable output of the completed Workflow.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> complete(Object output);

  /**
   * Waits for an event to be raised with name and returns the event data.
   *
   * @param eventName The name of the event to wait for. Event names are case-insensitive.
   *                  External event names can be reused any number of times; they are not
   *                  required to be unique.
   * @param timeout   The amount of time to wait before cancelling the external event task.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> waitForExternalEvent(String eventName, Duration timeout);
}
