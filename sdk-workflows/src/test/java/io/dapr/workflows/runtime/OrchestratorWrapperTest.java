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


import com.microsoft.durabletask.TaskOrchestrationContext;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrchestratorWrapperTest {
  public static class TestWorkflow extends Workflow {
    @Override
    public WorkflowStub create() {
      return WorkflowContext::getInstanceId;
    }
  }

  @Test
  public void getName() {
    OrchestratorWrapper<TestWorkflow> wrapper = new OrchestratorWrapper<>(TestWorkflow.class);
    Assertions.assertEquals(
        "io.dapr.workflows.runtime.OrchestratorWrapperTest.TestWorkflow",
        wrapper.getName()
    );
  }

  @Test
  public void createWithClass() {
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    OrchestratorWrapper<TestWorkflow> wrapper = new OrchestratorWrapper<>(TestWorkflow.class);
    when(mockContext.getInstanceId()).thenReturn("uuid");
    wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInstanceId();
  }

}