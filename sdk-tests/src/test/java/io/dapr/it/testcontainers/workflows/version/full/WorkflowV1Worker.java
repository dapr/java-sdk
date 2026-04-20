package io.dapr.it.testcontainers.workflows.version.full;

import io.dapr.it.testcontainers.workflows.multiapp.MultiAppWorkflow;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class WorkflowV1Worker {
  public static void main(String[] args) throws Exception {
    System.out.println("=== Starting Workflow V1 Runtime ===");

    // Register the Workflow with the builder
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();
    VersionedWorkflows.addWorkflowV1(builder, true);

    // Build and start the workflow runtime
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("WorkerV1 started");
      System.out.println("Waiting for workflow orchestration requests...");
      runtime.start();
    }
  }
}
