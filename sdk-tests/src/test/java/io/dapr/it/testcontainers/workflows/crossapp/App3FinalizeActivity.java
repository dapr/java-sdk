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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App3FinalizeActivity implements WorkflowActivity {

  private static final Logger logger = LoggerFactory.getLogger(App3FinalizeActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    String input = ctx.getInput(String.class);
    logger.info("=== App3: FinalizeActivity called ===");
    logger.info("Input: {}", input);
    
    String output = input + " [FINALIZED BY APP3]";
    logger.info("Output: {}", output);
    
    return output;
  }
}
