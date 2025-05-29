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

import io.dapr.durabletask.TaskFailedException;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.Duration;

public class BookTripWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
        return ctx -> {
            ctx.getLogger().info("Starting Workflow: " + ctx.getName());
            List<String> compensations = new ArrayList<>();

            // Define retry policy for compensation activities
            WorkflowTaskRetryPolicy compensationRetryPolicy = WorkflowTaskRetryPolicy.newBuilder()
                .setFirstRetryInterval(Duration.ofSeconds(1))
                .setMaxNumberOfAttempts(3)
                .build();

            WorkflowTaskOptions compensationOptions = new WorkflowTaskOptions(compensationRetryPolicy);

            try {
                // Book flight
                String flightResult = ctx.callActivity(BookFlightActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Flight booking completed: {}", flightResult);
                compensations.add("CancelFlight");

                // Book hotel
                String hotelResult = ctx.callActivity(BookHotelActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Hotel booking completed: {}", hotelResult);
                compensations.add("CancelHotel");

                // Book car
                String carResult = ctx.callActivity(BookCarActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Car booking completed: {}", carResult);
                compensations.add("CancelCar");

                String result = String.format("%s, %s, %s", flightResult, hotelResult, carResult);
                ctx.getLogger().info("Trip booked successfully: {}", result);
                ctx.complete(result);

            } catch (TaskFailedException e) {
                ctx.getLogger().info("******** executing compensation logic ********");
                ctx.getLogger().error("Activity failed: {}", e.getMessage());
                
                // Execute compensations in reverse order
                Collections.reverse(compensations);
                for (String compensation : compensations) {
                    try {
                        switch (compensation) {
                            case "CancelCar":
                                String carCancelResult = ctx.callActivity(
                                    CancelCarActivity.class.getName(), 
                                    null, 
                                    compensationOptions,
                                    String.class).await();
                                ctx.getLogger().info("Car cancellation completed: {}", carCancelResult);
                                break;

                            case "CancelHotel":
                                String hotelCancelResult = ctx.callActivity(
                                    CancelHotelActivity.class.getName(), 
                                    null, 
                                    compensationOptions,
                                    String.class).await();
                                ctx.getLogger().info("Hotel cancellation completed: {}", hotelCancelResult);
                                break;

                            case "CancelFlight":
                                String flightCancelResult = ctx.callActivity(
                                    CancelFlightActivity.class.getName(), 
                                    null, 
                                    compensationOptions,
                                    String.class).await();
                                ctx.getLogger().info("Flight cancellation completed: {}", flightCancelResult);
                                break;
                        }
                    } catch (TaskFailedException ex) {
                        // Only catch TaskFailedException for actual activity failures
                        ctx.getLogger().error("Activity failed during compensation: {}", ex.getMessage());
                    }
                }
                ctx.complete("Workflow failed, compensation applied");
            }
        };
    }
}
