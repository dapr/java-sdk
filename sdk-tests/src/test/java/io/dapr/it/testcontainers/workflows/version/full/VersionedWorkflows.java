package io.dapr.it.testcontainers.workflows.version.full;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class VersionedWorkflows {
  public static final String ACTIVITY_1 = "Activity1";
  public static final String ACTIVITY_2 = "Activity2";
  public static final String ACTIVITY_3 = "Activity3";
  public static final String ACTIVITY_4 = "Activity4";

  public static class FullVersionWorkflowV1 implements Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
        ctx.getLogger().info("Starting Workflow V1: {}", ctx.getName());

        String result = "";
        result += ctx.callActivity(VersionedWorkflows.ACTIVITY_1,  String.class).await() +", ";
        ctx.waitForExternalEvent("test").await();
        result += ctx.callActivity(VersionedWorkflows.ACTIVITY_2,  String.class).await();

        ctx.getLogger().info("Workflow finished with result: {}", result);
        ctx.complete(result);
      };
    }
  }

  public static class FullVersionWorkflowV2 implements Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
        ctx.getLogger().info("Starting Workflow V2: {}", ctx.getName());

        String result = "";
        result += ctx.callActivity(VersionedWorkflows.ACTIVITY_3,  String.class).await() +", ";
        result += ctx.callActivity(VersionedWorkflows.ACTIVITY_4,  String.class).await();

        ctx.getLogger().info("Workflow finished with result: {}", result);
        ctx.complete(result);
      };
    }
  }

  public static void addWorkflowV1(WorkflowRuntimeBuilder workflowRuntimeBuilder, boolean isLatest) {
    workflowRuntimeBuilder.registerWorkflow("VersionWorkflow",
        VersionedWorkflows.FullVersionWorkflowV1.class,
        "V1",
        isLatest);


    workflowRuntimeBuilder.registerActivity(VersionedWorkflows.ACTIVITY_1, (ctx -> {
      System.out.println("Activity1 called.");
      return VersionedWorkflows.ACTIVITY_1;
    }));

    workflowRuntimeBuilder.registerActivity(VersionedWorkflows.ACTIVITY_2, (ctx -> {
      System.out.println("Activity2 called.");
      return VersionedWorkflows.ACTIVITY_2;
    }));
  }

  public static void addWorkflowV2(WorkflowRuntimeBuilder workflowRuntimeBuilder) {
    workflowRuntimeBuilder.registerWorkflow("VersionWorkflow",
        VersionedWorkflows.FullVersionWorkflowV2.class,
        "V2",
        true);

    workflowRuntimeBuilder.registerActivity(VersionedWorkflows.ACTIVITY_3, (ctx -> {
      System.out.println("Activity3 called.");
      return VersionedWorkflows.ACTIVITY_3;
    }));

    workflowRuntimeBuilder.registerActivity(VersionedWorkflows.ACTIVITY_4, (ctx -> {
      System.out.println("Activity4 called.");
      return VersionedWorkflows.ACTIVITY_4;
    }));
  }
}
