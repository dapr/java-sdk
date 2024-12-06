package io.dapr.spring.boot.autoconfigure.client.workflows;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class TestWorkflow extends Workflow {

  @Override
  public WorkflowStub create() {
    return null;
  }
}
