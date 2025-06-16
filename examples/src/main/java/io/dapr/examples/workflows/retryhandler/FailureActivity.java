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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class FailureActivity implements WorkflowActivity {

  private static final Logger LOGGER = LoggerFactory.getLogger(FailureActivity.class);
  public static final long TIME_TO_SUCCESS = 10;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    LOGGER.info("Starting Activity: {}", ctx.getName());

    Instant timestamp = ctx.getInput(Instant.class);

    LOGGER.info("Input timestamp: {}", timestamp);
    if(timestamp.plusSeconds(TIME_TO_SUCCESS).isBefore(Instant.now())) {
      LOGGER.info("Completing Activity: {}", ctx.getName());
      return Instant.now();
    }

    LOGGER.info("Throwing exception for Activity: {}", ctx.getName());

    throw new RuntimeException("Failure!");
  }
}
