package io.dapr.spring.boot4.autoconfigure.client.workflows;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class TestActivity implements WorkflowActivity {

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    return "OK";
  }

  public RestTemplate getRestTemplate() {
    return restTemplate;
  }
}
