package io.dapr.spring.boot.autoconfigure.client.workflows;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

public class TestActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    return null;
  }
}
