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

package io.dapr.examples.workflows.crossapp;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

/**
 * App3 Worker - registers only the FinalizeActivity.
 * This app will handle cross-app activity calls from the main workflow.
 */
public class App3Worker {
  
  public static void main(String[] args) throws Exception {
    System.out.println("=== Starting App3Worker ===");
    // Register the Workflow with the builder
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
            .registerActivity(App3FinalizeActivity.class);

      // Build and start the workflow runtime
      try (WorkflowRuntime runtime = builder.build()) {
        System.out.println("App3 is ready to receive cross-app activity calls...");
        runtime.start();
      }
    }
}
