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

package io.dapr.examples.workflows.suspendresume;

import io.dapr.examples.workflows.externalevent.DemoExternalEventWorkflow;
import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.examples.workflows.utils.RetryUtils;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class DemoSuspendResumeClient {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) {
    try (DaprWorkflowClient client = new DaprWorkflowClient(PropertyUtils.getProperties(args))) {
      String instanceId = RetryUtils.callWithRetry(() -> client.scheduleNewWorkflow(DemoExternalEventWorkflow.class), Duration.ofSeconds(60));
      System.out.printf("Started a new external-event workflow with instance ID: %s%n", instanceId);


      System.out.printf("Suspending Workflow Instance: %s%n", instanceId );
      client.suspendWorkflow(instanceId, "suspending workflow instance.");

      WorkflowInstanceStatus instanceState = client.getInstanceState(instanceId, false);
      assert instanceState != null;
      System.out.printf("Workflow Instance Status: %s%n", instanceState.getRuntimeStatus().name() );

      System.out.printf("Let's resume the Workflow Instance before sending the external event: %s%n", instanceId );
      client.resumeWorkflow(instanceId, "resuming workflow instance.");

      instanceState = client.getInstanceState(instanceId, false);
      assert instanceState != null;
      System.out.printf("Workflow Instance Status: %s%n", instanceState.getRuntimeStatus().name() );

      System.out.printf("Now that the instance is RUNNING again, lets send the external event. %n");
      client.raiseEvent(instanceId, "Approval", true);

      client.waitForInstanceCompletion(instanceId, null, true);
      System.out.printf("workflow instance with ID: %s completed.", instanceId);

    } catch (TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
