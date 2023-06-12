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

package io.dapr.workflows.client;

import com.microsoft.durabletask.OrchestrationRuntimeStatus;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum describing the runtime status of a workflow.
 */
public enum WorkflowRuntimeStatus {
  /**
   * The workflow started running.
   */
  RUNNING,

  /**
   * The workflow completed normally.
   */
  COMPLETED,

  /**
   * The workflow is continued as new.
   */
  CONTINUED_AS_NEW,

  /**
   * The workflow completed with an unhandled exception.
   */
  FAILED,

  /**
   * The workflow was abruptly cancelled via a management API call.
   */
  CANCELED,

  /**
   * The workflow was abruptly terminated via a management API call.
   */
  TERMINATED,

  /**
   * The workflow was scheduled but hasn't started running.
   */
  PENDING,

  /**
   * The workflow was suspended.
   */
  SUSPENDED,

  /**
   * The status of the workflow is unknown.
   */
  UNKNOWN;

  /**
   * Convert runtime OrchestrationRuntimeStatus to WorkflowRuntimeStatus.
   *
   * @param status The OrchestrationRuntimeStatus to convert to WorkflowRuntimeStatus.
   * @return The runtime status of the workflow.
   */
  public static WorkflowRuntimeStatus fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus status) {

    if (status == null) {
      return WorkflowRuntimeStatus.UNKNOWN;
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
        return WorkflowRuntimeStatus.UNKNOWN;
    }
  }

  /**
   * Convert runtime WorkflowRuntimeStatus to OrchestrationRuntimeStatus.
   *
   * @param status The OrchestrationRuntimeStatus to convert to WorkflowRuntimeStatus.
   * @return The runtime status of the Orchestration.
   */
  public static OrchestrationRuntimeStatus toOrchestrationRuntimeStatus(WorkflowRuntimeStatus status) {

    switch (status) {
      case RUNNING:
        return OrchestrationRuntimeStatus.RUNNING;
      case COMPLETED:
        return OrchestrationRuntimeStatus.COMPLETED;
      case CONTINUED_AS_NEW:
        return OrchestrationRuntimeStatus.CONTINUED_AS_NEW;
      case FAILED:
        return OrchestrationRuntimeStatus.FAILED;
      case CANCELED:
        return OrchestrationRuntimeStatus.CANCELED;
      case TERMINATED:
        return OrchestrationRuntimeStatus.TERMINATED;
      case PENDING:
        return OrchestrationRuntimeStatus.PENDING;
      case SUSPENDED:
        return OrchestrationRuntimeStatus.SUSPENDED;
      default:
        return null;
    }
  }

  /**
   * Convert runtime WorkflowRuntimeStatus to OrchestrationRuntimeStatus.
   *
   * @param statuses The list of orchestrationRuntimeStatus to convert to a list of WorkflowRuntimeStatuses.
   * @return The list runtime status of the Orchestration.
   */
  public static List<OrchestrationRuntimeStatus> toOrchestrationRuntimeStatus(List<WorkflowRuntimeStatus> statuses) {
    return statuses.stream()
                   .map(x -> toOrchestrationRuntimeStatus(x)) 
                   .collect(Collectors.toList());
  }
}
