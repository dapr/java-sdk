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


import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.TaskOrchestrationFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class OrchestratorWrapperTest {
    public static class TestWorkflow extends Workflow{
      @Override
      public void run(WorkflowContext ctx) {
        ctx.getInstanceId();
      }
    }

  @Test
  public void getName() throws NoSuchMethodException {
    OrchestratorWrapper<TestWorkflow> wrapper = new OrchestratorWrapper(TestWorkflow.class);
    Assert.assertEquals(
        "io.dapr.workflows.runtime.OrchestratorWrapperTest.TestWorkflow",
        wrapper.getName()
    );
  }

  @Test
  public void createWithClass() throws NoSuchMethodException {
    TaskOrchestrationContext mockContext = mock(TaskOrchestrationContext.class);
    OrchestratorWrapper<Workflow> wrapper = new OrchestratorWrapper(TestWorkflow.class);
    when( mockContext.getInstanceId() ).thenReturn("uuid");
    wrapper.create().run(mockContext);
    verify(mockContext, times(1)).getInstanceId();
  }

}
