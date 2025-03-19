package io.dapr.workflows.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class NewWorkflowOptionsTest {

  @Test
  void testNewWorkflowOption() {
    NewWorkflowOptions workflowOption = new NewWorkflowOptions();
    String version = "v1";
    String instanceId = "123";
    Object input = new Object();
    Instant startTime = Instant.now();

    workflowOption.setVersion(version)
        .setInstanceId(instanceId)
        .setInput(input)
        .setStartTime(startTime);

    Assertions.assertEquals(version, workflowOption.getVersion());
    Assertions.assertEquals(instanceId, workflowOption.getInstanceId());
    Assertions.assertEquals(input, workflowOption.getInput());
    Assertions.assertEquals(startTime, workflowOption.getStartTime());
  }
}
