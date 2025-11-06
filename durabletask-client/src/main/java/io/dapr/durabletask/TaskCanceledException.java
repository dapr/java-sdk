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

//@TODO: This should inherit from Exception, not TaskFailedException

/**
 * Represents a task cancellation, either because of a timeout or because of an explicit cancellation operation.
 */
public final class TaskCanceledException extends TaskFailedException {
  // Only intended to be created within this package
  TaskCanceledException(String message, String taskName, int taskId) {
    super(message, taskName, taskId, new FailureDetails(TaskCanceledException.class.getName(), message, "", true));
  }
}
