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

package io.dapr.examples.workflows.continueasnew;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import java.time.Duration;

public class DemoContinueAsNewWorkflow extends Workflow {
  /*
  Compared with a CRON schedule, this periodic workflow example will never overlap.
  For example, a CRON schedule that executes a cleanup every hour will execute it at 1:00, 2:00, 3:00 etc.
  and could potentially run into overlap issues if the cleanup takes longer than an hour.
  In this example, however, if the cleanup takes 30 minutes, and we create a timer for 1 hour between cleanups,
  then it will be scheduled at 1:00, 2:30, 4:00, etc. and there is no chance of overlap.
   */
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      ctx.getLogger().info("call CleanUpActivity to do the clean up");
      ctx.callActivity(CleanUpActivity.class.getName()).await();
      ctx.getLogger().info("CleanUpActivity finished");

      ctx.getLogger().info("wait 10 seconds for next clean up");
      ctx.createTimer(Duration.ofSeconds(10)).await();

      // continue the workflow.
      ctx.continueAsNew(null);
    };
  }
}
