package io.dapr.spring.boot4.autoconfigure.client.workflows;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class TestWorkflow implements Workflow {

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.callActivity(TestActivity.class.getName(), null).await();
    };
  }

  public RestTemplate getRestTemplate() {
    return restTemplate;
  }
}
