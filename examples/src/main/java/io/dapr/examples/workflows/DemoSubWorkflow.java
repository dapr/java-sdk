/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.examples.workflows;

import io.dapr.workflows.runtime.Workflow;
import io.dapr.workflows.runtime.WorkflowContext;


/**
 * Implementation of the DemoWorkflow for the server side.
 */
public class DemoSubWorkflow extends Workflow {
  @Override
  public void run(WorkflowContext ctx) {

    var logger = ctx.getLogger();
    logger.info("Child-Workflow> Started: " + ctx.getName());
    logger.info("Child-Workflow> Instance ID: " + ctx.getInstanceId());
    logger.info("Child-Workflow> Current Time: " + ctx.getCurrentInstant());
    
    var input = ctx.getInput(String.class);
    logger.info("Child-Workflow> Input: " + input);

    logger.info("Child-Workflow> Completed");
    ctx.complete("result: " + input);
  }
}
