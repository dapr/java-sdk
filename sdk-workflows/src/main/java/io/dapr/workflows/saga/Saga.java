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

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.interruption.ContinueAsNewInterruption;
import com.microsoft.durabletask.interruption.OrchestratorBlockedException;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowExecutionOptions;

import java.util.ArrayList;
import java.util.List;

public final class Saga {
  private final SagaOptions options;
  private final List<CompensationInformation> compensationActivities = new ArrayList<>();

  /**
   * Build up a Saga with its options.
   * 
   * @param options Saga option.
   */
  public Saga(SagaOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("option is required and should not be null.");
    }
    this.options = options;
  }

  /**
   * Register a compensation activity.
   * 
   * @param activityClassName name of the activity class
   * @param activityInput     input of the activity to be compensated
   */
  public void registerCompensation(String activityClassName, Object activityInput) {
    this.registerCompensation(activityClassName, activityInput, null);
  }

  /**
   * Register a compensation activity.
   *
   * @param activityClassName name of the activity class
   * @param activityInput     input of the activity to be compensated
   * @param options           task options to set retry strategy
   */
  public void registerCompensation(String activityClassName, Object activityInput, WorkflowExecutionOptions options) {
    if (activityClassName == null || activityClassName.isEmpty()) {
      throw new IllegalArgumentException("activityClassName is required and should not be null or empty.");
    }
    this.compensationActivities.add(new CompensationInformation(activityClassName, activityInput, options));
  }

  /**
   * Compensate all registered activities.
   * 
   * @param ctx Workflow context.
   */
  public void compensate(WorkflowContext ctx) {
    // Check if parallel compensation is enabled
    // Special case: when parallel compensation is enabled and there is only one
    // compensation, we still
    // compensate sequentially.
    if (options.isParallelCompensation() && compensationActivities.size() > 1) {
      compensateInParallel(ctx);
    } else {
      compensateSequentially(ctx);
    }
  }

  private void compensateInParallel(WorkflowContext ctx) {
    List<Task<Void>> tasks = new ArrayList<>(compensationActivities.size());
    for (CompensationInformation compensationActivity : compensationActivities) {
      Task<Void> task = executeCompensateActivity(ctx, compensationActivity);
      tasks.add(task);
    }

    try {
      ctx.allOf(tasks).await();
    } catch (Exception e) {
      throw new SagaCompensationException("Failed to compensate in parallel.", e);
    }
  }

  private void compensateSequentially(WorkflowContext ctx) {
    SagaCompensationException sagaException = null;
    for (int i = compensationActivities.size() - 1; i >= 0; i--) {
      String activityClassName = compensationActivities.get(i).getCompensationActivityClassName();
      try {
        executeCompensateActivity(ctx, compensationActivities.get(i)).await();
      } catch (OrchestratorBlockedException | ContinueAsNewInterruption e) {
        throw e;
      } catch (Exception e) {
        if (sagaException == null) {
          sagaException = new SagaCompensationException(
              "Exception in saga compensation: activity=" + activityClassName, e);
        } else {
          sagaException.addSuppressed(e);
        }

        if (!options.isContinueWithError()) {
          throw sagaException;
        }
      }
    }

    if (sagaException != null) {
      throw sagaException;
    }
  }

  private Task<Void> executeCompensateActivity(WorkflowContext ctx, CompensationInformation info)
      throws SagaCompensationException {
    String activityClassName = info.getCompensationActivityClassName();
    return ctx.callActivity(activityClassName, info.getCompensationActivityInput(),
        info.getExecutionOptions());
  }
}
