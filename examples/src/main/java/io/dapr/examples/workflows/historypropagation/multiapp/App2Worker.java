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
 * App2 worker - registers only App2AuditActivity. Receives cross-app activity
 * calls (with propagated history) from app1.
 */
public class App2Worker {

  public static void main(String[] args) throws Exception {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
        .registerActivity(App2AuditActivity.class);

    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("App2Worker started - registered App2AuditActivity only");
      runtime.start();
    }
  }
}
