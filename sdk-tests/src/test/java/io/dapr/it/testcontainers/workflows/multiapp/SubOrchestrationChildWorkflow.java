/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.it.testcontainers.workflows.multiapp;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;

/**
 * Child workflow that runs on app2 as a sub-orchestration.
 * It calls a local activity to transform the input and returns the result.
 */
public class SubOrchestrationChildWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      logger.info("Starting SubOrchestrationChildWorkflow: {}", ctx.getName());

      String input = ctx.getInput(String.class);
      logger.info("Child workflow input: {}", input);

      // Call a local activity within app2
      String transformed = ctx.callActivity(
          ChildTransformActivity.class.getName(), input, String.class
      ).await();

      logger.info("Child workflow transformed result: {}", transformed);
      ctx.complete(transformed);
    };
  }
}
