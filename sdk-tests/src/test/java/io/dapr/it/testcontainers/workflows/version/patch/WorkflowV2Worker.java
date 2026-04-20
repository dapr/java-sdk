package io.dapr.it.testcontainers.workflows.version.patch;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class WorkflowV2Worker {
  public static void main(String[] args) throws Exception {
    System.out.println("=== Starting Patch Workflow Runtime ===");

    // Register the Workflow with the builder
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();
    PatchedWorkflows.addPatchWorkflow(builder);

    // Build and start the workflow runtime
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Patch Worker started");
      System.out.println("Waiting for workflow orchestration requests...");
      runtime.start();
    }
  }
}
