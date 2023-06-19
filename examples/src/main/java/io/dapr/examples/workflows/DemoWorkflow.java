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

import com.microsoft.durabletask.CompositeTaskFailedException;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskCanceledException;
import io.dapr.workflows.runtime.Workflow;
import io.dapr.workflows.runtime.WorkflowContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the DemoWorkflow for the server side.
 */
public class DemoWorkflow extends Workflow {
  @Override
  public void run(WorkflowContext ctx) {
    ctx.getLogger().info("Starting Workflow: " + ctx.getName());
    ctx.getLogger().info("Instance ID: " + ctx.getInstanceId());
    ctx.getLogger().info("Current Orchestration Time: " + ctx.getCurrentInstant());
    ctx.getLogger().info("Waiting for event: 'TimedOutEvent'...");
    try {
      ctx.waitForExternalEvent("TimedOutEvent", Duration.ofSeconds(10)).await();
    } catch (TaskCanceledException e) {
      ctx.getLogger().warn("Timed out");
      ctx.getLogger().warn(e.getMessage());
    }

    ctx.getLogger().info("Waiting for event: 'TestEvent'...");
    try {
      ctx.waitForExternalEvent("TestEvent", Duration.ofSeconds(10)).await();
      ctx.getLogger().info("Received TestEvent");
    } catch (TaskCanceledException e) {
      ctx.getLogger().warn("Timed out");
      ctx.getLogger().warn(e.getMessage());
    }

    ctx.getLogger().info("Parallel Execution - Waiting for all tasks to finish...");
    try {
      Task<String> t1 = ctx.waitForExternalEvent("event1", Duration.ofSeconds(5), String.class);
      Task<String> t2 = ctx.waitForExternalEvent("event2", Duration.ofSeconds(5), String.class);
      Task<String> t3 = ctx.waitForExternalEvent("event3", Duration.ofSeconds(5), String.class);

      List<String> results = ctx.allOf(Arrays.asList(t1, t2, t3)).await();
      results.forEach(t -> ctx.getLogger().info("finished task: " + t));
      ctx.getLogger().info("All tasks finished!");

    } catch (CompositeTaskFailedException e) {
      ctx.getLogger().warn(e.getMessage());
      List<Exception> exceptions = e.getExceptions();
      exceptions.forEach(ex -> ctx.getLogger().warn(ex.getMessage()));
    }

    ctx.getLogger().info("Parallel Execution - Waiting for any task to finish...");
    try {
      Task<String> e1 = ctx.waitForExternalEvent("e1", Duration.ofSeconds(5), String.class);
      Task<String> e2 = ctx.waitForExternalEvent("e2", Duration.ofSeconds(5), String.class);
      Task<String> e3 = ctx.waitForExternalEvent("e3", Duration.ofSeconds(5), String.class);
      Task<Void> timeoutTask = ctx.createTimer(Duration.ofSeconds(1));

      Task<?> winner = ctx.anyOf(Arrays.asList(e1, e2, e3, timeoutTask)).await();
      if (winner == timeoutTask) {
        ctx.getLogger().info("All tasks timed out!");
      } else {
        ctx.getLogger().info("One of the tasks finished!");
      }
    } catch (TaskCanceledException e) {
      ctx.getLogger().warn("Timed out");
      ctx.getLogger().warn(e.getMessage());
    }

    ctx.getLogger().info("Calling Activity...");
    var input = new DemoActivityInput("Hello Activity!");
    var output = ctx.callActivity(DemoWorkflowActivity.class.getName(), input, DemoActivityOutput.class).await();

    ctx.getLogger().info("Activity returned: " + output);
    ctx.getLogger().info("Activity returned: " + output.getNewMessage());
    ctx.getLogger().info("Activity returned: " + output.getOriginalMessage());


    ctx.getLogger().info("Child-Workflow> Calling ChildWorkflow...");
    var childWorkflowInput = "Hello ChildWorkflow!";
    var childWorkflowOutput = ctx.callSubWorkflow(DemoSubWorkflow.class.getName(), childWorkflowInput, 
          String.class).await();

    ctx.getLogger().info("Child-Workflow> returned: " + childWorkflowOutput);

    ctx.getLogger().info("Workflow finished");
    ctx.complete("finished");
  }
}
