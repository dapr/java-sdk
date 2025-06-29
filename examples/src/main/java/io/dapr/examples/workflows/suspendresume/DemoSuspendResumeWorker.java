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

import io.dapr.examples.workflows.externalevent.ApproveActivity;
import io.dapr.examples.workflows.externalevent.DemoExternalEventWorkflow;
import io.dapr.examples.workflows.externalevent.DenyActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

public class DemoSuspendResumeWorker {
  /**
   * The main method of this app.
   *
   * @param args The port the app will listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    // Register the Workflow with the builder.
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(DemoExternalEventWorkflow.class);
    builder.registerActivity(ApproveActivity.class);
    builder.registerActivity(DenyActivity.class);

    // Build and then start the workflow runtime pulling and executing tasks
    WorkflowRuntime runtime = builder.build();
    System.out.println("Start workflow runtime");
    runtime.start();
  }
}
