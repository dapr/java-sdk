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

package io.dapr.examples.workflows;

import io.dapr.workflows.client.DaprWorkflowClient;

import java.util.concurrent.TimeUnit;

/**
 * For setup instructions, see the README.
 */
public class DemoWorkflowClient {

  /**
   * The main method.
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) throws InterruptedException {
    DaprWorkflowClient client = new DaprWorkflowClient();

    try (client) {
      System.out.println("*****");
      String workflowName = DemoWorkflow.class.getCanonicalName();
      String instanceId = client.scheduleNewWorkflow(workflowName);
      System.out.printf("Started new workflow instance with random ID: %s%n", instanceId);

      System.out.println("Sleep and allow this workflow instance to timeout...");
      TimeUnit.SECONDS.sleep(10);

      System.out.println("*****");
      String instanceToTerminateId = "terminateMe";
      client.scheduleNewWorkflow(workflowName, null, instanceToTerminateId);
      System.out.printf("Started new workflow instance with specified ID: %s%n", instanceToTerminateId);

      TimeUnit.SECONDS.sleep(5);
      System.out.println("Terminate this workflow instance manually before the timeout is reached");
      client.terminateWorkflow(instanceToTerminateId, null);
      System.out.println("*****");
    }

    System.out.println("Exiting DemoWorkflowClient.");
    System.exit(0);
  }
}
