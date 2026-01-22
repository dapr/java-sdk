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
 * Interface that provides {@link TaskActivity} implementations with activity context, such as an activity's name and
 * its input.
 */
public interface TaskActivityContext {
  /**
   * Gets the name of the current task activity.
   *
   * @return the name of the current task activity
   */
  String getName();

  /**
   * Gets the deserialized activity input.
   *
   * @param targetType the {@link Class} object associated with {@code T}
   * @param <T>        the target type to deserialize the input into
   * @return the deserialized activity input value
   */
  <T> T getInput(Class<T> targetType);

  /**
   * Gets the execution id of the current task activity.
   *
   * @return the execution id of the current task activity
   */
  String getTaskExecutionId();

  /**
   * Gets the task id of the current task activity.
   *
   * @return the task id of the current task activity
   */
  int getTaskId();
}
