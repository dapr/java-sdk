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

package io.dapr.examples.workflows.management;

import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.RerunWorkflowFromEventOptions;
import io.dapr.workflows.client.WorkflowHistoryEvent;
import io.dapr.workflows.client.WorkflowInstancePage;
import io.dapr.workflows.client.WorkflowState;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class DemoWorkflowManagementClient {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprWorkflowClient client = new DaprWorkflowClient(PropertyUtils.getProperties(args))) {
      String instanceId = client.scheduleNewWorkflow(DemoWorkflowManagementWorkflow.class);
      System.out.printf("Started a new workflow with instance ID: %s%n", instanceId);

      WorkflowState state = client.waitForWorkflowCompletion(instanceId, null, true);
      System.out.printf("Workflow completed with result: %s%n", state.readOutputAs(String.class));

      // Read the full execution history.
      List<WorkflowHistoryEvent> history = client.getInstanceHistory(instanceId);
      System.out.printf("History for %s has %d events:%n", instanceId, history.size());
      for (WorkflowHistoryEvent event : history) {
        System.out.printf("  eventId=%d type=%s at=%s%n",
            event.getEventId(), event.getEventType(), event.getTimestamp());
      }

      // Rerun the workflow from the first history event.
      int firstEventId = history.get(0).getEventId();
      String rerunId = client.rerunWorkflowFromEvent(instanceId, firstEventId,
          new RerunWorkflowFromEventOptions().setInput("Osaka").setOverwriteInput(true));
      System.out.printf("Reran workflow from event %d as new instance: %s%n", firstEventId, rerunId);
      client.waitForWorkflowCompletion(rerunId, null, true);

      // List workflow instance IDs (first page).
      WorkflowInstancePage page = client.listInstanceIds(null, 100);
      System.out.printf("Listed %d instance ID(s); continuationToken=%s%n",
          page.getInstanceIds().size(), page.getContinuationToken());
    } catch (TimeoutException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
