package io.dapr.workflows.runtime;


import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WorkflowRuntimeBuilderTest {
  public static class TestWorkflow extends Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
      };
    }
  }

  public static class TestActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext ctx) {
      return null;
    }
  }

  @Test
  public void registerValidWorkflowClass() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerWorkflow(TestWorkflow.class));
  }

  @Test
  public void registerValidWorkflowActivityClass() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerActivity(TestActivity.class));
  }

  @Test
  public void buildTest() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().build());
  }
}
