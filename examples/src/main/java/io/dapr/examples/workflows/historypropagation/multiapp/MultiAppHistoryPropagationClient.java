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

package io.dapr.examples.workflows.historypropagation.multiapp;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;

import java.util.concurrent.TimeoutException;

/**
 * Client that starts the multi-app history propagation workflow on app1.
 */
public class MultiAppHistoryPropagationClient {

  public static void main(String[] args) {
    String input = args.length >= 1 ? args[0] : "payment-1234";
    System.out.println("Starting multi-app history propagation client with input: " + input);

    try (DaprWorkflowClient client = new DaprWorkflowClient()) {
      String instanceId = client.scheduleNewWorkflow(App1Workflow.class, input);
      System.out.printf("Started App1Workflow with instance ID: %s%n", instanceId);

      WorkflowState state = client.waitForWorkflowCompletion(instanceId, null, true);
      String result = state.readOutputAs(String.class);
      System.out.printf("Workflow %s completed with result: %s%n", instanceId, result);

    } catch (TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
