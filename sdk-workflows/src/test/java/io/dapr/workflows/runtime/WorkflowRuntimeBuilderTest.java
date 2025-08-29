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
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.WorkflowStub;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WorkflowRuntimeBuilderTest {
  public static class TestWorkflow implements Workflow {
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
  public void registerValidWorkflowInstance() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerWorkflow(new TestWorkflow()));
  }

  @Test
  public void registerValidWorkflowActivityClass() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerActivity(TestActivity.class));
  }

  @Test
  public void registerValidWorkflowActivityInstance() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerActivity(new TestActivity()));
  }

  @Test
  public void buildTest() {
    assertDoesNotThrow(() -> {
      try {
        WorkflowRuntime runtime = new WorkflowRuntimeBuilder().build();
        System.out.println("WorkflowRuntime created");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void loggingOutputTest() {
    // Set the output stream for log capturing
    ByteArrayOutputStream outStreamCapture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStreamCapture));

    Logger testLogger = mock(Logger.class);

    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder(testLogger).registerWorkflow(TestWorkflow.class));
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder(testLogger).registerActivity(TestActivity.class));

    WorkflowRuntimeBuilder workflowRuntimeBuilder = new WorkflowRuntimeBuilder();

    WorkflowRuntime runtime = workflowRuntimeBuilder.build();
    verify(testLogger, times(1))
        .info(eq("Registered Workflow: {}"), eq("TestWorkflow"));

    verify(testLogger, times(1))
        .info(eq("Registered Activity: {}"), eq("TestActivity"));
  }
}
