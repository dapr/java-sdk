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


import com.microsoft.durabletask.DurableTaskGrpcWorker;
import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WorkflowRuntimeTest {
    public static class TestWorkflow extends Workflow {
      @Override
      public WorkflowStub create() {
        return ctx -> { };
      }
    }

  @Test
  public void startTest() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    try (WorkflowRuntime runtime = new WorkflowRuntime(worker)) {
      assertDoesNotThrow(() -> {
        runtime.start(false);
      });
    }
  }

  @Test
  public void closeWithoutStarting() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    try (WorkflowRuntime runtime = new WorkflowRuntime(worker)) {
      assertDoesNotThrow(runtime::close);
    }
  }
}
