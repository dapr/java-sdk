package io.dapr.it.testcontainers.workflows.version.patch;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class PatchedWorkflows {
  public static final String ACTIVITY_1 = "Activity1";
  public static final String ACTIVITY_2 = "Activity2";
  public static final String ACTIVITY_3 = "Activity3";
  public static final String ACTIVITY_4 = "Activity4";
  public static final String NAME = "VersionWorkflow";


  public static class SimmpleWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
        ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

        String result = "";
        result += ctx.callActivity(PatchedWorkflows.ACTIVITY_1,  String.class).await() +", ";
        ctx.waitForExternalEvent("test").await();
        result += ctx.callActivity(PatchedWorkflows.ACTIVITY_2,  String.class).await();

        ctx.getLogger().info("Workflow finished with result: {}", result);
        ctx.complete(result);
      };
    }
  }


  public static class PatchSimpleWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
        ctx.getLogger().info("Starting Workflow with patch V2: {}", ctx.getName());

        String result = "";
        result += ctx.callActivity(PatchedWorkflows.ACTIVITY_1, String.class).await() +", ";

        var isPatched = ctx.isPatched("V2");
        if(isPatched) {
          result += ctx.callActivity(PatchedWorkflows.ACTIVITY_3, String.class).await() + ", ";
          result += ctx.callActivity(PatchedWorkflows.ACTIVITY_4, String.class).await() + ", ";
        }

        ctx.waitForExternalEvent("test").await();
        result += ctx.callActivity(PatchedWorkflows.ACTIVITY_2, String.class).await();
        ctx.getLogger().info("Workflow finished with result: {}", result);
        ctx.complete(result);
      };
    }
  }

  public static void addSimpleWorkflow(WorkflowRuntimeBuilder workflowRuntimeBuilder) {
    workflowRuntimeBuilder.registerWorkflow(NAME, SimmpleWorkflow.class);

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_1, (ctx -> {
      System.out.println("Activity1 called.");
      return PatchedWorkflows.ACTIVITY_1;
    }));

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_2, (ctx -> {
      System.out.println("Activity2 called.");
      return PatchedWorkflows.ACTIVITY_2;
    }));
  }

  public static void addPatchWorkflow(WorkflowRuntimeBuilder workflowRuntimeBuilder) {
    workflowRuntimeBuilder.registerWorkflow(NAME,
        PatchSimpleWorkflow.class);

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_1, (ctx -> {
      System.out.println("Activity1 called.");
      return PatchedWorkflows.ACTIVITY_1;
    }));

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_2, (ctx -> {
      System.out.println("Activity2 called.");
      return PatchedWorkflows.ACTIVITY_2;
    }));

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_3, (ctx -> {
      System.out.println("Activity3 called.");
      return PatchedWorkflows.ACTIVITY_3;
    }));

    workflowRuntimeBuilder.registerActivity(PatchedWorkflows.ACTIVITY_4, (ctx -> {
      System.out.println("Activity4 called.");
      return PatchedWorkflows.ACTIVITY_4;
    }));
  }
}
