package io.dapr.it.testcontainers.workflows.version.full;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class WorkflowV2Worker {
  public static void main(String[] args) throws Exception {
    System.out.println("=== Starting Workflow V2 Runtime ===");

    // Register the Workflow with the builder
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();
    VersionedWorkflows.addWorkflowV1(builder, false);
    VersionedWorkflows.addWorkflowV2(builder);

    // Build and start the workflow runtime
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("WorkerV2 started");
      System.out.println("Waiting for workflow orchestration requests...");
      runtime.start();
    }
  }
}
