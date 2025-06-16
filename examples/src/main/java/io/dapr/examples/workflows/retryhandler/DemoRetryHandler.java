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

import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowTaskRetryContext;
import io.dapr.workflows.WorkflowTaskRetryHandler;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class DemoRetryHandler implements WorkflowTaskRetryHandler {

  @Override
  public boolean handle(WorkflowTaskRetryContext retryContext) {
    WorkflowContext workflowContext = retryContext.getWorkflowContext();
    Logger logger = retryContext.getWorkflowContext().getLogger();
    Object input = retryContext.getInput();
    String taskName = retryContext.getTaskName();

    if(taskName.equalsIgnoreCase(FailureActivity.class.getName())) {
      logger.info("FailureActivity Input: {}", input);
      Instant timestampInput = (Instant) input;
      // Add a second to ensure, it is 100% passed the time to success
      Instant timeToSuccess = timestampInput.plusSeconds(FailureActivity.TIME_TO_SUCCESS + 1);
      long timeToWait = timestampInput.until(timeToSuccess, TimeUnit.SECONDS.toChronoUnit());

      logger.info("Waiting {} seconds before retrying.", timeToWait);
      workflowContext.createTimer(Duration.ofSeconds(timeToWait)).await();
      logger.info("Send request to FailureActivity");
    }

    return true;
  }
}
