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
 * limitations under the License.
*/

package io.dapr.it.testcontainers.workflows.multiapp;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import org.slf4j.Logger;

public class MultiAppWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String instanceId = ctx.getInstanceId();
      logger.info("Starting MultiAppWorkflow: {}", ctx.getName());
      logger.info("Instance ID: {}", instanceId);

      String input = ctx.getInput(String.class);
      logger.info("Workflow input: {}", input);

      // Call App2TransformActivity in app2
      logger.info("Calling multi-app activity in 'app2'...");
      String transformedByApp2 = ctx.callActivity(
          App2TransformActivity.class.getName(), 
          input,
          new WorkflowTaskOptions("app2"),
          String.class
      ).await();

      // Call App3FinalizeActivity in app3
      logger.info("Calling multi-app activity in 'app3'...");
      String finalizedByApp3 = ctx.callActivity(
          App3FinalizeActivity.class.getName(), 
          transformedByApp2,
          new WorkflowTaskOptions("app3"),
          String.class
      ).await();

      logger.info("Final multi-app activity result: {}", finalizedByApp3);
      ctx.complete(finalizedByApp3);
    };
  }
}
