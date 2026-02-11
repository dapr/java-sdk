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

package io.dapr.springboot4.examples.wfp.suspendresume;

import io.dapr.springboot.examples.wfp.externalevent.Decision;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

@Component
public class SuspendResumeWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      ctx.callActivity(PerformTaskActivity.class.getName(), String.class).await();

      ctx.getLogger().info("Waiting for approval...");
      Boolean approved = ctx.waitForExternalEvent("Approval", boolean.class).await();

      ctx.getLogger().info("approval-event arrived");

      ctx.callActivity(PerformTaskActivity.class.getName(), String.class).await();

      ctx.complete(new Decision(approved));
    };
  }
}
