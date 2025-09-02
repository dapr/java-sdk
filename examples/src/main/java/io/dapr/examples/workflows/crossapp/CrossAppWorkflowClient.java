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

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;

import java.util.concurrent.TimeoutException;

/**
 * Cross-App Workflow Client - starts and monitors workflows.
 * 
 * 1. Create a workflow client
 * 2. Start a new workflow instance
 * 3. Wait for completion and get results
 */
public class CrossAppWorkflowClient {
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: CrossAppWorkflowClientExample <input>");
      System.out.println("Example: CrossAppWorkflowClientExample \"Hello World\"");
      return;
    }
    
    String input = args[0];
    System.out.println("=== Starting Cross-App Workflow Client ===");
    System.out.println("Input: " + input);
    
    try (DaprWorkflowClient client = new DaprWorkflowClient()) {
      System.out.println("Created DaprWorkflowClient successfully");
      
      // Start a new workflow instance
      System.out.println("Attempting to start new workflow...");
      String instanceId = client.scheduleNewWorkflow(CrossAppWorkflow.class, input);
      System.out.printf("Started a new cross-app workflow with instance ID: %s%n", instanceId);
      
      // Wait for the workflow to complete
      System.out.println("Waiting for workflow completion...");
      WorkflowInstanceStatus workflowInstanceStatus = 
          client.waitForInstanceCompletion(instanceId, null, true);
      
      // Get the result
      String result = workflowInstanceStatus.readOutputAs(String.class);
      System.out.printf("Workflow instance with ID: %s completed with result: %s%n", instanceId, result);
      
    } catch (TimeoutException | InterruptedException e) {
      System.err.println("Error waiting for workflow completion: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Error creating workflow client or starting workflow: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
