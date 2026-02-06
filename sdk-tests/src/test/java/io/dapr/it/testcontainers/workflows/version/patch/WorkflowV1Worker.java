package io.dapr.it.testcontainers.workflows.version.patch;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class WorkflowV1Worker {
  public static void main(String[] args) throws Exception {
    System.out.println("=== Starting Simple Workflow Runtime ===");

    // Register the Workflow with the builder
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder();
    PatchedWorkflows.addSimpleWorkflow(builder);

    // Build and start the workflow runtime
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Simple Worker started");
      System.out.println("Waiting for workflow orchestration requests...");
      runtime.start();
    }
  }
}
