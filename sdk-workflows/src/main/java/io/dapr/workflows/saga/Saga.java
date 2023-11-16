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

package io.dapr.workflows.saga;

import com.microsoft.durabletask.OrchestratorBlockedException;
import com.microsoft.durabletask.Task;
import io.dapr.workflows.WorkflowContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class Saga {
  private final SagaConfiguration config;
  private final List<CompensatationInformation> compensationActivities = new ArrayList<>();

  /**
   * Build up a Saga with its config.
   * 
   * @param config Saga configuration.
   */
  public Saga(SagaConfiguration config) {
    if (config == null) {
      throw new IllegalArgumentException("config is required and should not be null.");
    }
    this.config = config;
  }

  /**
   * Register a compensation activity.
   * 
   * @param activityClassName name of the activity class
   * @param activityInput     input of the activity to be compensated
   */
  public void registerCompensation(String activityClassName, Object activityInput) {
    if (activityClassName == null || activityClassName.isEmpty()) {
      throw new IllegalArgumentException("activityClassName is required and should not be null or empty.");
    }
    this.compensationActivities.add(new CompensatationInformation(activityClassName, activityInput));
  }

  /**
   * Compensate all registered activities.
   * 
   * @param ctx Workflow context.
   */
  public void compensate(WorkflowContext ctx) {
    // Check if parallel compensation is enabled
    // Specical case: when parallel compensation is enabled and there is only one
    // compensation, we still
    // compensate sequentially.
    if (config.isParallelCompensation() && compensationActivities.size() > 1) {
      compensateInParallel(ctx);
    } else {
      compensateSequentially(ctx);
    }
  }

  private void compensateInParallel(WorkflowContext ctx) {
    // thread number should be limited by maxParallelThread
    int threadNumber = compensationActivities.size();
    if (threadNumber > config.getMaxParallelThread()) {
      threadNumber = config.getMaxParallelThread();
    }

    ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
    List<Callable<String>> compensationTasks = new ArrayList<>();
    for (CompensatationInformation compensationActivity : compensationActivities) {
      Callable<String> compensationTask = new Callable<String>() {
        @Override
        public String call() {
          return executeCompensateActivity(ctx, compensationActivity);
        }
      };
      compensationTasks.add(compensationTask);
    }

    List<Future<String>> resultFutures;
    try {
      // TBD: hard code timeout to 60 seconds in the first version
      resultFutures = executor.invokeAll(compensationTasks, 60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new SagaCompensationException("Failed to compensate in parallel.", e);
    }
    SagaCompensationException sagaException = null;
    for (Future<String> resultFuture : resultFutures) {
      try {
        resultFuture.get();
      } catch (Exception e) {
        if (sagaException == null) {
          sagaException = new SagaCompensationException("Failed to compensate in parallel.", e);
        } else {
          sagaException.addSuppressed(e);
        }
      }
    }

    if (sagaException != null) {
      throw sagaException;
    }
  }

  private void compensateSequentially(WorkflowContext ctx) {
    SagaCompensationException sagaException = null;
    for (int i = compensationActivities.size() - 1; i >= 0; i--) {
      try {
        executeCompensateActivity(ctx, compensationActivities.get(i));
      } catch (SagaCompensationException e) {
        if (sagaException == null) {
          sagaException = e;
        } else {
          sagaException.addSuppressed(e);
        }

        if (!config.isContinueWithError()) {
          throw sagaException;
        }
      }
    }

    if (sagaException != null) {
      throw sagaException;
    }
  }

  private String executeCompensateActivity(WorkflowContext ctx, CompensatationInformation context)
      throws SagaCompensationException {
    String activityClassName = context.getCompensatationActivityClassName();
    try {
      Task<Void> task = ctx.callActivity(activityClassName, context.getCompensatationActivityInput());
      if (task != null) {
        task.await();
      }
      // return activityClassName for logs and tracing
      return activityClassName;
    } catch (OrchestratorBlockedException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new SagaCompensationException("Exception in saga compensatation: activity=" + activityClassName, e);
    }
  }
}
