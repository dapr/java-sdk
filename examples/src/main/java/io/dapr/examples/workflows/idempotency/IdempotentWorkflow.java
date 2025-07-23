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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;


public class IdempotentWorkflow implements Workflow {


  private static Map<String, AtomicInteger> keyStore;
    
    
    public static Map<String, AtomicInteger> getKeyStore() {
        if (keyStore == null) {
          synchronized (IdempotentWorkflow.class) {
              if (keyStore == null) {
                keyStore = new ConcurrentHashMap<>();
              }
          }
      }
      return keyStore;
  }


  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      var result = new ArrayList<Integer>();

      WorkflowTaskOptions options = new WorkflowTaskOptions(WorkflowTaskRetryPolicy.newBuilder()
      .setMaxNumberOfAttempts(10)
      .setFirstRetryInterval(Duration.ofSeconds(1))
      .setMaxRetryInterval(Duration.ofSeconds(10))
      .setBackoffCoefficient(2.0)  
      .setRetryTimeout(Duration.ofSeconds(10))
      .build());

      result.add(ctx.callActivity(IdempotentActivity.class.getName(), 3, options, Integer.class).await());
      result.add(ctx.callActivity(IdempotentActivity.class.getName(), 2, options, Integer.class).await());
      result.add(ctx.callActivity(IdempotentActivity.class.getName(), 1, options, Integer.class).await());

      result.forEach(r -> ctx.getLogger().info("Result: " + r));

      ctx.complete(result);
    };
  }
}