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

package io.dapr.workflows.runtime;

import io.dapr.durabletask.OrchestrationRuntimeStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;

public class WorkflowRuntimeStatusConverter {

  private WorkflowRuntimeStatusConverter() {
  }

  /**
   * Converts an OrchestrationRuntimeStatus to a WorkflowRuntimeStatus.
   *
   * @param status the OrchestrationRuntimeStatus to convert
   * @return the corresponding WorkflowRuntimeStatus
   * @throws IllegalArgumentException if the status is null or unknown
   */
  public static WorkflowRuntimeStatus fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("status cannot be null");
    }

    switch (status) {
      case RUNNING:
        return WorkflowRuntimeStatus.RUNNING;
      case COMPLETED:
        return WorkflowRuntimeStatus.COMPLETED;
      case CONTINUED_AS_NEW:
        return WorkflowRuntimeStatus.CONTINUED_AS_NEW;
      case FAILED:
        return WorkflowRuntimeStatus.FAILED;
      case CANCELED:
        return WorkflowRuntimeStatus.CANCELED;
      case TERMINATED:
        return WorkflowRuntimeStatus.TERMINATED;
      case PENDING:
        return WorkflowRuntimeStatus.PENDING;
      case SUSPENDED:
        return WorkflowRuntimeStatus.SUSPENDED;
      default:
        throw new IllegalArgumentException(String.format("Unknown status value: %s", status));
    }
  }

}
