package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.spring.boot.autoconfigure.client.workflows.TestActivity;
import io.dapr.spring.boot.autoconfigure.client.workflows.TestWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {WorkflowTestApplication.class, DaprClientAutoConfiguration.class, TestActivity.class, TestWorkflow.class})
public class DaprWorkflowsRegistrationTests {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private WorkflowRuntimeBuilder workflowRuntimeBuilder;

  @Autowired
  private TestActivity testActivity;

  @Autowired
  private TestWorkflow testWorkflow;

  @Test
  public void testWorkflowInjection(){

    //I cannot test here if the client works, as it needs the runtime
    assertNotNull(daprWorkflowClient);

    //@TODO: there is no way to assert the runtime and its registered workflows and activities
    assertNotNull(workflowRuntimeBuilder);

    //Check that both Activities and Workflows are managed beans
    assertNotNull(testActivity);
    assertNotNull(testWorkflow);
    assertNotNull(testActivity.getRestTemplate());
    assertNotNull(testWorkflow.getRestTemplate());

  }
}
