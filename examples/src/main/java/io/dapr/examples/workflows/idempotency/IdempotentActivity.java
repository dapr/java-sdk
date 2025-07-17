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

package io.dapr.examples.workflows.idempotency;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IdempotentActivity implements WorkflowActivity {

  Logger logger = LoggerFactory.getLogger(IdempotentActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {

    logger.info("[{}] Starting Activity {} ", ctx.getTaskExecutionId(), ctx.getName());
    var limit = ctx.getInput(Integer.class);

    var counter = IdempotentWorkflow.getKeyStore().getOrDefault(ctx.getTaskExecutionId(), new AtomicInteger(0));
    if (counter.get() != limit) {
      logger.info("Task execution key[{}] with limit {}, incrementing counter {}",ctx.getTaskExecutionId(), limit, counter.get());
      IdempotentWorkflow.getKeyStore().put(ctx.getTaskExecutionId(), new AtomicInteger(counter.incrementAndGet()));

      throw new IllegalStateException("Task execution key not found");
    }
    
    return counter.get();
  }
}
