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

package io.dapr.it.testcontainers.workflows.crossapp;

import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;

/**
 * App2Worker - registers the App2TransformActivity.
 * This app will handle cross-app activity calls from the main workflow.
 */
public class App2Worker {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting App2Worker (App2TransformActivity) ===");
        
        // Register the Activity with the builder
        WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder()
            .registerActivity(App2TransformActivity.class);
        
        // Build and start the workflow runtime
        try (WorkflowRuntime runtime = builder.build()) {
            System.out.println("App2Worker started - registered App2TransformActivity only");
            System.out.println("App2 is ready to receive cross-app activity calls...");
            System.out.println("Waiting for cross-app activity calls...");
            runtime.start();
        }
    }
}
