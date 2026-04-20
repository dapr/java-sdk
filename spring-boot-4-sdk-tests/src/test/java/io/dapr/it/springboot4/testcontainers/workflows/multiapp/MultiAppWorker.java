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
 * limitations under the License.
*/

package io.dapr.it.springboot4.testcontainers.workflows.multiapp;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

/**
 * MultiAppWorker - registers only the MultiAppWorkflow.
 * This is the main workflow orchestrator that will call activities in other apps.
 */
public class MultiAppWorker {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting MultiAppWorker (Workflow Orchestrator) ===");
        
        // Register the Workflow with the builder
        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
            .registerWorkflow(MultiAppWorkflow.class);
        
        // Build and start the workflow runtime
        try (WorkflowRuntime runtime = builder.build()) {
            System.out.println("MultiAppWorker started - registered MultiAppWorkflow only");
            System.out.println("Waiting for workflow orchestration requests...");
            runtime.start();
        }
    }
}

