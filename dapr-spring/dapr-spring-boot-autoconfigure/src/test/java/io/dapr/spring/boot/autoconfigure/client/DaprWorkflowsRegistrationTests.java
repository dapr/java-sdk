package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.spring.boot.autoconfigure.client.workflows.TestActivity;
import io.dapr.spring.boot.autoconfigure.client.workflows.TestWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {WorkflowTestApplication.class, DaprClientAutoConfiguration.class, TestActivity.class, TestWorkflow.class})
public class DaprWorkflowsRegistrationTests {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Test
  public void doThat(){
    assertNotNull(daprWorkflowClient);
    
  }
}
