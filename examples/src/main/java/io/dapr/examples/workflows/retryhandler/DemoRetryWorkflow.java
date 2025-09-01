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

package io.dapr.examples.workflows.retryhandler;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryHandler;
import org.slf4j.Logger;

import java.time.Instant;

public class DemoRetryWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return context -> {
      Logger logger = context.getLogger();
      logger.info("Starting RetryWorkflow: {}", context.getName());

      WorkflowTaskRetryHandler retryHandler = new DemoRetryHandler();
      WorkflowTaskOptions taskOptions = new WorkflowTaskOptions(retryHandler);

      logger.info("RetryWorkflow is calling Activity: {}", FailureActivity.class.getName());
      Instant currentTime = context.getCurrentInstant();
      Instant result = context.callActivity(FailureActivity.class.getName(), currentTime, taskOptions, Instant.class).await();

      logger.info("RetryWorkflow finished with: {}",  result);
      context.complete(result);
    };
  }
}
