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

package io.dapr.it.testcontainers.workflows.multiapp;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

/**
 * Worker that registers {@link CrossAppEventWorkflow}. It is started for the host app only, so any
 * instance of that workflow can only be running on the host app.
 */
public class CrossAppEventWorker {
  public static void main(String[] args) throws Exception {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
        .registerWorkflow(CrossAppEventWorkflow.class);
    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("CrossAppEventWorker started - registered CrossAppEventWorkflow");
      runtime.start();
    }
  }
}
