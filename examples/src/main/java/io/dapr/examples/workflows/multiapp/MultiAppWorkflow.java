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

package io.dapr.examples.workflows.multiapp;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;

/**
 * Example workflow that demonstrates cross-app activity calls.
 * This workflow calls activities in different apps using the appId parameter.
 */
public class MultiAppWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      var logger = ctx.getLogger();
      logger.info("=== WORKFLOW STARTING ===");
      logger.info("Starting MultiAppWorkflow: {}", ctx.getName());
      logger.info("Workflow name: {}", ctx.getName());
      logger.info("Workflow instance ID: {}", ctx.getInstanceId());

      String input = ctx.getInput(String.class);
      logger.info("MultiAppWorkflow received input: {}", input);
      logger.info("Workflow input: {}", input);

      // Call an activity in another app by passing in an active appID to the WorkflowTaskOptions
      logger.info("Calling multi-app activity in 'app2'...");
      logger.info("About to call multi-app activity in app2...");
      String multiAppResult = ctx.callActivity(
          App2TransformActivity.class.getName(),
          input,
          new WorkflowTaskOptions("app2"),
          String.class
      ).await();

      // Call another activity in a different app
      logger.info("Calling multi-app activity in 'app3'...");
      logger.info("About to call multi-app activity in app3...");
      String finalResult = ctx.callActivity(
          App3FinalizeActivity.class.getName(),
          multiAppResult,
          new WorkflowTaskOptions("app3"),
          String.class
      ).await();
      logger.info("Final multi-app activity result: {}", finalResult);

      logger.info("MultiAppWorkflow finished with: {}", finalResult);
      logger.info("=== WORKFLOW COMPLETING WITH: {} ===", finalResult);
      ctx.complete(finalResult);
    };
  }
}
