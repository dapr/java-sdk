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

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskFailedException;
import com.microsoft.durabletask.TaskOptions;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;

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
   * @return the name of the current workflow
   */
  String getName();

  /**
   * Gets the instance ID of the current workflow.
   *
   * @return the instance ID of the current workflow
   */
  String getInstanceId();

  /**
   * Gets the current orchestration time in UTC.
   *
   * @return the current orchestration time in UTC
   */
  Instant getCurrentInstant();

  /**
   * Completes the current workflow.
   *
   * @param output the serializable output of the completed Workflow.
   */
  void complete(Object output);

  /**
   * Waits for an event to be raised with name and returns the event data.
   *
   * @param eventName The name of the event to wait for. Event names are case-insensitive.
   *                  External event names can be reused any number of times; they are not
   *                  required to be unique.
   * @param timeout   The amount of time to wait before cancelling the external event task.
   * @return Asynchronous task to {@code await()}.
   */
  Task waitForExternalEvent(String eventName, Duration timeout);


  /**
   * Asynchronously invokes an activity by name and with the specified input value and returns a new {@link Task}
   * that completes when the activity completes. If the activity completes successfully, the returned {@code Task}'s
   * value will be the activity's output. If the activity fails, the returned {@code Task} will complete exceptionally
   * with a {@link TaskFailedException}.
   *
   * @param name       the name of the activity to call
   * @param input      the serializable input to pass to the activity
   * @param options    additional options that control the execution and processing of the activity
   * @param returnType the expected class type of the activity output
   * @param <V>        the expected type of the activity output
   * @return a new {@link Task} that completes when the activity completes or fails
   */
  <V> Task<V> callActivity(String name, Object input, TaskOptions options, Class<V> returnType);

  /**
   * Asynchronously invokes an activity by name and returns a new {@link Task} that completes when the activity
   * completes. See {@link #callActivity(String, Object, TaskOptions, Class)} for a complete description.
   *
   * @param name the name of the activity to call
   * @return a new {@link Task} that completes when the activity completes or fails
   * @see #callActivity(String, Object, TaskOptions, Class)
   */
  default Task<Void> callActivity(String name) {
    return this.callActivity(name, null, null, Void.class);
  }

  /**
   * Asynchronously invokes an activity by name and with the specified input value and returns a new {@link Task}
   * that completes when the activity completes. See {@link #callActivity(String, Object, TaskOptions, Class)} for a
   * complete description.
   *
   * @param name  the name of the activity to call
   * @param input the serializable input to pass to the activity
   * @return a new {@link Task} that completes when the activity completes or fails
   */
  default Task<Void> callActivity(String name, Object input) {
    return this.callActivity(name, input, null, Void.class);
  }

  /**
   * Asynchronously invokes an activity by name and returns a new {@link Task} that completes when the activity
   * completes. If the activity completes successfully, the returned {@code Task}'s value will be the activity's
   * output. See {@link #callActivity(String, Object, TaskOptions, Class)} for a complete description.
   *
   * @param name       the name of the activity to call
   * @param returnType the expected class type of the activity output
   * @param <V>        the expected type of the activity output
   * @return a new {@link Task} that completes when the activity completes or fails
   */
  default <V> Task<V> callActivity(String name, Class<V> returnType) {
    return this.callActivity(name, null, null, returnType);
  }

  /**
   * Asynchronously invokes an activity by name and with the specified input value and returns a new {@link Task}
   * that completes when the activity completes.If the activity completes successfully, the returned {@code Task}'s
   * value will be the activity's output. See {@link #callActivity(String, Object, TaskOptions, Class)} for a
   * complete description.
   *
   * @param name       the name of the activity to call
   * @param input      the serializable input to pass to the activity
   * @param returnType the expected class type of the activity output
   * @param <V>        the expected type of the activity output
   * @return a new {@link Task} that completes when the activity completes or fails
   */
  default <V> Task<V> callActivity(String name, Object input, Class<V> returnType) {
    return this.callActivity(name, input, null, returnType);
  }

  /**
   * Asynchronously invokes an activity by name and with the specified input value and returns a new {@link Task}
   * that completes when the activity completes. See {@link #callActivity(String, Object, TaskOptions, Class)} for a
   * complete description.
   *
   * @param name    the name of the activity to call
   * @param input   the serializable input to pass to the activity
   * @param options additional options that control the execution and processing of the activity
   * @return a new {@link Task} that completes when the activity completes or fails
   */
  default Task<Void> callActivity(String name, Object input, TaskOptions options) {
    return this.callActivity(name, input, options, Void.class);
  }

  /**
   * Gets a value indicating whether the workflow is currently replaying a previous execution.
   *
   * <p>Workflow functions are "replayed" after being unloaded from memory to reconstruct local variable state.
   * During a replay, previously executed tasks will be completed automatically with previously seen values
   * that are stored in the workflow history. Once the workflow reaches the point where it's no longer
   * replaying existing history, this method will return {@code false}.
   *
   * <p>You can use this method if you have logic that needs to run only when <em>not</em> replaying. For example,
   * certain types of application logging may become too noisy when duplicated as part of replay. The
   * application code could check to see whether the function is being replayed and then issue the log statements
   * when this value is {@code false}.
   *
   * @return {@code true} if the workflow is replaying, otherwise {@code false}
   */
  boolean getIsReplaying();
}
