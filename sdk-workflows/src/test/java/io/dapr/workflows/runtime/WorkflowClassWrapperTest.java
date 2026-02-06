/*
 * Copyright 2023 The Dapr Authors
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

import io.dapr.durabletask.TaskOrchestrationContext;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkflowClassWrapperTest {
  public static class TestWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  public static abstract class TestErrorWorkflow implements Workflow {
    public TestErrorWorkflow(String s){}
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  public static abstract class TestPrivateWorkflow implements Workflow {
    private TestPrivateWorkflow(){}
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  @Test
  public void getName() {
    WorkflowClassWrapper<TestWorkflow> wrapper = new WorkflowClassWrapper<>(TestWorkflow.class);

    assertEquals(
        "io.dapr.workflows.runtime.WorkflowClassWrapperTest.TestWorkflow",
        wrapper.getName()
    );
  }

  @Test
  public void createWithClass() {
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    WorkflowClassWrapper<TestWorkflow> wrapper = new WorkflowClassWrapper<>(TestWorkflow.class);

    when(mockContext.getInstanceId()).thenReturn("uuid");
    wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInstanceId();
  }

  @Test
  public void createWithClassAndVersion() {
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    WorkflowClassWrapper<TestWorkflow> wrapper = new WorkflowClassWrapper<>("TestWorkflow", TestWorkflow.class, "v1",false);
    when(mockContext.getInstanceId()).thenReturn("uuid");
    wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInstanceId();
  }

  @Test
  public void createErrorClassAndVersion() {
      assertThrowsExactly(RuntimeException.class, () -> new WorkflowClassWrapper<>(TestErrorWorkflow.class));
      assertThrowsExactly(RuntimeException.class, () -> new WorkflowClassWrapper<>("TestErrorWorkflow", TestErrorWorkflow.class, "v1",false));

    WorkflowClassWrapper<TestPrivateWorkflow> wrapper = new WorkflowClassWrapper<>("TestPrivateWorkflow", TestPrivateWorkflow.class, "v2",false);
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    assertThrowsExactly(RuntimeException.class, () -> wrapper.create().run(mockContext));

  }

}
