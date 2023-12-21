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

package io.dapr.examples.workflows.faninout;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class DemoFanInOutClient {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) throws InterruptedException {
    try (DaprWorkflowClient client = new DaprWorkflowClient()) {
      // The input is an arbitrary list of strings.
      List<String> listOfStrings = Arrays.asList(
          "Hello, world!",
          "The quick brown fox jumps over the lazy dog.",
          "If a tree falls in the forest and there is no one there to hear it, does it make a sound?",
          "The greatest glory in living lies not in never falling, but in rising every time we fall.",
          "Always remember that you are absolutely unique. Just like everyone else.");

      // Schedule an orchestration which will reliably count the number of words in all the given sentences.
      String instanceId = client.scheduleNewWorkflow(
          DemoFanInOutWorkflow.class,
          listOfStrings);
      System.out.printf("Started a new fan out/fan in model workflow with instance ID: %s%n", instanceId);

      // Block until the orchestration completes. Then print the final status, which includes the output.
      WorkflowInstanceStatus workflowInstanceStatus = client.waitForInstanceCompletion(
          instanceId,
          Duration.ofSeconds(30),
          true);
      System.out.printf("workflow instance with ID: %s completed with result: %s%n", instanceId,
          workflowInstanceStatus.readOutputAs(int.class));
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
