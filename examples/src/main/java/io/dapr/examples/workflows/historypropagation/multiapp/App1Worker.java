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
limitations under the License.
*/

package io.dapr.examples.workflows.historypropagation.multiapp;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

/**
 * App1 worker - registers only App1Workflow.
 */
public class App1Worker {

  public static void main(String[] args) throws Exception {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
        .registerWorkflow(App1Workflow.class);

    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("App1Worker started - registered App1Workflow only");
      runtime.start();
    }
  }
}
