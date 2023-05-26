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
import java.util.concurrent.TimeoutException;

/**
 * For setup instructions, see the README.
 */
public class DemoWorkflowClient {

  /**
   * The main method.
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) throws InterruptedException, TimeoutException {
    DaprWorkflowClient client = new DaprWorkflowClient();

    try (client) {
      String workflowName = DemoWorkflow.class.getCanonicalName();
      String instanceId = client.scheduleNewWorkflow(workflowName);

      System.out.printf("Started new workflow instance: %s%n", instanceId);

      // EXAMPLE OF OTHER METHOD CALLS
      // TimeUnit.SECOND.sleep(5);
      // client.raiseEvent("myEvent", instanceId);
      //
      // String instanceToTerminateId = client.scheduleNewWorkflow(workflowName);
      // client.terminate(instanceToTerminateId);

    }

    System.out.println("Exiting DemoWorkflowClient.");
    System.exit(0);
  }
}
