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

import com.microsoft.durabletask.TaskCanceledException;
import io.dapr.workflows.runtime.Workflow;
import io.dapr.workflows.runtime.WorkflowStub;

import java.time.Duration;

/**
 * Implementation of the DemoWorkflow for the server side.
 */
public class DemoWorkflow extends Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName().block());
      ctx.getLogger().info("Instance ID: " + ctx.getInstanceId().block());
      ctx.getLogger().info("Waiting for event: 'myEvent'...");
      try {
        ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(10)).block();
        ctx.getLogger().info("Received!");
      } catch (TaskCanceledException e) {
        ctx.getLogger().warn("Timed out");
        ctx.getLogger().warn(e.getMessage());
      }
      ctx.complete("finished").block();
    };
  }
}
