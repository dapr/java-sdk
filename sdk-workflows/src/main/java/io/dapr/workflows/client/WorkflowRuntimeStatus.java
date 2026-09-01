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

import io.dapr.durabletask.implementation.protobuf.Orchestration;

import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_CANCELED;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_CONTINUED_AS_NEW;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_FAILED;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_PENDING;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_RUNNING;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_STALLED;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_SUSPENDED;
import static io.dapr.durabletask.implementation.protobuf.Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_TERMINATED;

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
   * The workflow is in a stalled state.
   */
  STALLED;

  /**
   * Maps a protobuf workflow status onto this enum.
   *
   * @param status the protobuf status to convert.
   * @return the corresponding {@link WorkflowRuntimeStatus}.
   * @throws IllegalArgumentException if the status is unknown.
   */
  public static WorkflowRuntimeStatus fromProtobuf(Orchestration.OrchestrationStatus status) {
    switch (status) {
      case ORCHESTRATION_STATUS_RUNNING:
        return RUNNING;
      case ORCHESTRATION_STATUS_COMPLETED:
        return COMPLETED;
      case ORCHESTRATION_STATUS_CONTINUED_AS_NEW:
        return CONTINUED_AS_NEW;
      case ORCHESTRATION_STATUS_FAILED:
        return FAILED;
      case ORCHESTRATION_STATUS_CANCELED:
        return CANCELED;
      case ORCHESTRATION_STATUS_TERMINATED:
        return TERMINATED;
      case ORCHESTRATION_STATUS_PENDING:
        return PENDING;
      case ORCHESTRATION_STATUS_SUSPENDED:
        return SUSPENDED;
      case ORCHESTRATION_STATUS_STALLED:
        return STALLED;
      default:
        throw new IllegalArgumentException(String.format("Unknown status value: %s", status));
    }
  }

  /**
   * Maps this enum onto its protobuf workflow status.
   *
   * @param status the status to convert.
   * @return the corresponding protobuf status.
   * @throws IllegalArgumentException if the status is unknown.
   */
  public static Orchestration.OrchestrationStatus toProtobuf(WorkflowRuntimeStatus status) {
    switch (status) {
      case RUNNING:
        return ORCHESTRATION_STATUS_RUNNING;
      case COMPLETED:
        return ORCHESTRATION_STATUS_COMPLETED;
      case CONTINUED_AS_NEW:
        return ORCHESTRATION_STATUS_CONTINUED_AS_NEW;
      case FAILED:
        return ORCHESTRATION_STATUS_FAILED;
      case CANCELED:
        return ORCHESTRATION_STATUS_CANCELED;
      case TERMINATED:
        return ORCHESTRATION_STATUS_TERMINATED;
      case PENDING:
        return ORCHESTRATION_STATUS_PENDING;
      case SUSPENDED:
        return ORCHESTRATION_STATUS_SUSPENDED;
      case STALLED:
        return ORCHESTRATION_STATUS_STALLED;
      default:
        throw new IllegalArgumentException(String.format("Unknown status value: %s", status));
    }
  }
}
