package io.dapr.workflows.runtime;

import com.microsoft.durabletask.OrchestrationRuntimeStatus;
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
