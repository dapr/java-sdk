/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.examples.workflows.schedule;

import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowScheduleOptions;
import io.dapr.workflows.client.WorkflowScheduleState;
import io.dapr.workflows.client.WorkflowScheduleStatus;

public class DemoScheduleClient {
  private static final String SCHEDULE_NAME = "daily-report";

  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   * @throws InterruptedException if interrupted while waiting for the schedule to complete.
   */
  public static void main(String[] args) throws InterruptedException {
    try (DaprWorkflowClient client = new DaprWorkflowClient(PropertyUtils.getProperties(args))) {
      client.createWorkflowSchedule(SCHEDULE_NAME, DemoScheduleWorkflow.class, "@every 5s",
          new WorkflowScheduleOptions().setInput("sales").setRepeats(3).setOverwrite(true));
      System.out.printf("Created schedule '%s'%n", SCHEDULE_NAME);

      // A triggered run counts towards the repeats, so two scheduled runs follow it.
      client.triggerWorkflowSchedule(SCHEDULE_NAME);
      System.out.println("Triggered run");

      WorkflowScheduleStatus status;
      do {
        Thread.sleep(2000);
        status = client.getWorkflowSchedule(SCHEDULE_NAME).getStatus();
        System.out.printf("Schedule state: %s runs=%d%n", status.getState(), status.getRunCount());
      } while (status.getState() == WorkflowScheduleState.ACTIVE);

      // Every run is a workflow instance whose ID starts with the schedule name.
      int runs = client.listInstanceIds(null, null, SCHEDULE_NAME + "_").getInstanceIds().size();
      System.out.printf("Runs: %d%n", runs);

      client.deleteWorkflowSchedule(SCHEDULE_NAME);
      System.out.printf("Deleted schedule '%s'%n", SCHEDULE_NAME);
    }
  }
}
