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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookHotelActivity implements WorkflowActivity {
    private static final Logger logger = LoggerFactory.getLogger(BookHotelActivity.class);

    @Override
    public String run(WorkflowActivityContext ctx) {
        logger.info("Starting Activity: " + ctx.getName());
        logger.info("Simulating hotel booking process...");

        // Simulate some work
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = "Hotel booked successfully";
        logger.info("Activity completed with result: " + result);
        return result;
    }
}
