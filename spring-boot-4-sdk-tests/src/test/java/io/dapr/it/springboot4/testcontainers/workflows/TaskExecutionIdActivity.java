/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.it.springboot4.testcontainers.workflows;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

public class TaskExecutionIdActivity implements WorkflowActivity {

  @Override
  public Object run(WorkflowActivityContext ctx) {
    TestWorkflowPayload workflowPayload = ctx.getInput(TestWorkflowPayload.class);
    KeyStore keyStore = KeyStore.getInstance();
    Boolean exists = keyStore.getKey(ctx.getTaskExecutionId());
    if (!Boolean.TRUE.equals(exists)) {
      keyStore.addKey(ctx.getTaskExecutionId(), true);
      workflowPayload.getPayloads().add("Execution key not found");
      throw new IllegalStateException("Task execution key not found");
    }
    workflowPayload.getPayloads().add("Execution key found");
    return workflowPayload;
  }

}
