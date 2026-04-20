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

public class BookTripWorkflow implements Workflow {
    @Override
    public WorkflowStub create() {
        return ctx -> {
            ctx.getLogger().info("Starting Workflow: " + ctx.getName());
            CompensationHelper compensationHelper = new CompensationHelper();

            try {
                // Book flight
                String flightResult = ctx.callActivity(
                    BookFlightActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Flight booking completed: {}", flightResult);
                compensationHelper.addCompensation("CancelFlight", () ->
                    ctx.callActivity(CancelFlightActivity.class.getName(), null, String.class).await());

                // Book hotel
                String hotelResult = ctx.callActivity(
                    BookHotelActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Hotel booking completed: {}", hotelResult);
                compensationHelper.addCompensation("CancelHotel", () ->
                    ctx.callActivity(CancelHotelActivity.class.getName(), null, String.class).await());

                // Book car
                String carResult = ctx.callActivity(
                    BookCarActivity.class.getName(), null, String.class).await();
                ctx.getLogger().info("Car booking completed: {}", carResult);
                compensationHelper.addCompensation("CancelCar", () ->
                    ctx.callActivity(CancelCarActivity.class.getName(), null, String.class).await());

                String result = String.format("%s, %s, %s", flightResult, hotelResult, carResult);
                ctx.getLogger().info("Trip booked successfully: {}", result);
                ctx.complete(result);

            } catch (TaskFailedException e) {
                ctx.getLogger().info("******** executing compensation logic ********");
                ctx.getLogger().error("Activity failed: {}", e.getMessage());
                compensationHelper.compensate();
                ctx.complete("Workflow failed, compensation applied");
            }
        };
    }
}
