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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkflowInstanceWrapperTest {
  public static class TestWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  @Test
  public void getName() {
    WorkflowInstanceWrapper<TestWorkflow> wrapper = new WorkflowInstanceWrapper<>(new TestWorkflow());

    assertEquals(
        "io.dapr.workflows.runtime.WorkflowInstanceWrapperTest.TestWorkflow",
        wrapper.getName()
    );
  }

  @Test
  public void createWithInstance() {
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    WorkflowInstanceWrapper<TestWorkflow> wrapper = new WorkflowInstanceWrapper<>(new TestWorkflow());

    when(mockContext.getInstanceId()).thenReturn("uuid");
    wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInstanceId();
  }

}
