/*
 * Copyright 2024 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/
package io.dapr.workflows.runtime;


import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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

  @Test
  public void loggingOutputTest() {
    // Set the output stream for log capturing
    ByteArrayOutputStream outStreamCapture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStreamCapture));

    Logger testLogger = Mockito.mock(Logger.class);

    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder(testLogger).registerWorkflow(TestWorkflow.class));
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder(testLogger).registerActivity(TestActivity.class));

    WorkflowRuntimeBuilder wfRuntime = new WorkflowRuntimeBuilder();

    wfRuntime.build();

    Mockito.verify(testLogger, Mockito.times(1))
        .info(Mockito.eq("Registered Workflow: TestWorkflow"));
    Mockito.verify(testLogger, Mockito.times(1))
        .info(Mockito.eq("Registered Activity: TestActivity"));
  }

}
