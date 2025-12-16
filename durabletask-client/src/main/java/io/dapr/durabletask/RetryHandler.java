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
 * Functional interface for implementing custom task retry handlers.
 *
 * <p>It's important to remember that retry handler code is an extension of the orchestrator code and must
 * therefore comply with all the determinism requirements of orchestrator code.</p>
 */
@FunctionalInterface
public interface RetryHandler {
  /**
   * Invokes the retry handler logic and returns a value indicating whether to continue retrying.
   *
   * @param context retry context that's updated between each retry attempt
   * @return {@code true} to continue retrying or {@code false} to stop retrying.
   */
  boolean handle(RetryContext context);
}
