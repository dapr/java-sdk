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

package io.dapr.examples.workflows.compensation;

import io.dapr.examples.workflows.utils.PropertyUtils;
import io.dapr.examples.workflows.utils.RetryUtils;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class BookTripClient {
    public static void main(String[] args) {
        try (DaprWorkflowClient client = new DaprWorkflowClient(PropertyUtils.getProperties(args))) {
            String instanceId = RetryUtils.callWithRetry(() -> client.scheduleNewWorkflow(BookTripWorkflow.class), Duration.ofSeconds(60));
            System.out.printf("Started a new trip booking workflow with instance ID: %s%n", instanceId);

            WorkflowState status = client.waitForWorkflowCompletion(instanceId, Duration.ofMinutes(30), true);
            System.out.printf("Workflow instance with ID: %s completed with status: %s%n", instanceId, status);
            System.out.printf("Workflow output: %s%n", status.getSerializedOutput());
        } catch (TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
