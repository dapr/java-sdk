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

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;

import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_CANCELED;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_CONTINUED_AS_NEW;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_FAILED;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_PENDING;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_RUNNING;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_SUSPENDED;
import static io.dapr.durabletask.implementation.protobuf.OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_TERMINATED;

/**
 * Enum describing the runtime status of the orchestration.
 */
public enum OrchestrationRuntimeStatus {
  /**
   * The orchestration started running.
   */
  RUNNING,

  /**
   * The orchestration completed normally.
   */
  COMPLETED,

  /**
   * The orchestration is transitioning into a new instance.
   * This status value is obsolete and exists only for compatibility reasons.
   */
  CONTINUED_AS_NEW,

  /**
   * The orchestration completed with an unhandled exception.
   */
  FAILED,

  /**
   * The orchestration canceled gracefully.
   * The Canceled status is not currently used and exists only for compatibility reasons.
   */
  CANCELED,

  /**
   * The orchestration was abruptly terminated via a management API call.
   */
  TERMINATED,

  /**
   * The orchestration was scheduled but hasn't started running.
   */
  PENDING,

  /**
   * The orchestration is in a suspended state.
   */
  SUSPENDED,

  /**
   * The orchestration is in a stalled state.
   */
  STALLED;

  static OrchestrationRuntimeStatus fromProtobuf(OrchestratorService.OrchestrationStatus status) {
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

  static OrchestratorService.OrchestrationStatus toProtobuf(OrchestrationRuntimeStatus status) {
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
      default:
        throw new IllegalArgumentException(String.format("Unknown status value: %s", status));
    }
  }
}
