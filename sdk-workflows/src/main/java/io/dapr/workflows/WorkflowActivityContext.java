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
 * limitations under the License.
 */

package io.dapr.workflows;

import io.dapr.durabletask.PropagatedHistory;
import org.slf4j.Logger;

import java.util.Optional;

public interface WorkflowActivityContext {

  Logger getLogger();

  String getName();

  String getTaskExecutionId();

  <T> T getInput(Class<T> targetType);

  String getTraceParent();

  /**
   * Gets the propagated history from a parent workflow, if any was propagated.
   *
   * @return an Optional containing the propagated history, or empty if none was propagated
   */
  Optional<PropagatedHistory> getPropagatedHistory();
}
