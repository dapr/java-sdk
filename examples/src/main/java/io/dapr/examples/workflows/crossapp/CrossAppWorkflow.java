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

package io.dapr.examples.workflows.crossapp;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;

/**
 * Example workflow that demonstrates cross-app activity calls.
 * This workflow calls activities in different apps using the appId parameter.
 */
public class CrossAppWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      System.out.println("=== WORKFLOW STARTING ===");
      ctx.getLogger().info("Starting CrossAppWorkflow: " + ctx.getName());
      System.out.println("Workflow name: " + ctx.getName());
      System.out.println("Workflow instance ID: " + ctx.getInstanceId());

      String input = ctx.getInput(String.class);
      ctx.getLogger().info("CrossAppWorkflow received input: " + input);
      System.out.println("Workflow input: " + input);

      // Call an activity in another app by passing in an active appID to the WorkflowTaskOptions
      ctx.getLogger().info("Calling cross-app activity in 'app2'...");
      System.out.println("About to call cross-app activity in app2...");
      String crossAppResult = ctx.callActivity(
          App2TransformActivity.class.getName(),
          input,
          new WorkflowTaskOptions("app2"),
          String.class
      ).await();

      // Call another activity in a different app
      ctx.getLogger().info("Calling cross-app activity in 'app3'...");
      System.out.println("About to call cross-app activity in app3...");
      String finalResult = ctx.callActivity(
          App3FinalizeActivity.class.getName(),
          crossAppResult,
          new WorkflowTaskOptions("app3"),
          String.class
      ).await();
      ctx.getLogger().info("Final cross-app activity result: " + finalResult);
      System.out.println("Final cross-app activity result: " + finalResult);
      
      ctx.getLogger().info("CrossAppWorkflow finished with: " + finalResult);
      System.out.println("=== WORKFLOW COMPLETING WITH: " + finalResult + " ===");
      ctx.complete(finalResult);
    };
  }
}
