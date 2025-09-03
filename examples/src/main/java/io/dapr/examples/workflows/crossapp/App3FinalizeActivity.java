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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

/**
 * FinalizeActivity for App3 - adds final processing.
 * This activity is called cross-app from the main workflow.
 */
public class App3FinalizeActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext context) {
        String input = context.getInput(String.class);
        var logger = context.getLogger();
        logger.info("=== App3: FinalizeActivity called ===");
        logger.info("Input: {}", input);
        String result = input + " [FINALIZED BY APP3]";
        logger.info("Output: {}", result);
        return result;
    }
}
