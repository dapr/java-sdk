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

package io.dapr.workflows;

import com.microsoft.durabletask.interruption.OrchestratorBlockedException;
import io.dapr.workflows.saga.SagaCompensationException;
import io.dapr.workflows.saga.SagaOption;

/**
 * Common interface for workflow implementations.
 */
public abstract class Workflow {
  public Workflow() {
  }

  /**
   * Executes the workflow logic.
   *
   * @return A WorkflowStub.
   */
  public abstract WorkflowStub create();

  /**
   * Executes the workflow logic.
   *
   * @param ctx provides access to methods for scheduling durable tasks and
   *            getting information about the current
   *            workflow instance.
   */
  public void run(WorkflowContext ctx) {
    WorkflowStub stub = this.create();

    if (!this.isSagaEnabled()) {
      // saga disabled
      stub.run(ctx);
    } else {
      // saga enabled
      try {
        stub.run(ctx);
      } catch (OrchestratorBlockedException e) {
        throw e;
      } catch (SagaCompensationException e) {
        // Saga compensation is triggered gracefully but failed in exception
        // don't need to trigger compensation again
        throw e;
      } catch (Exception e) {
        try {
          ctx.compensate();
        } catch (Exception se) {
          se.addSuppressed(e);
          throw se;
        }

        throw e;
      }
    }
  }

  public boolean isSagaEnabled() {
    return this.getSagaOption() != null;
  }

  /**
   * get saga configuration.
   * 
   * @return saga configuration
   */
  public SagaOption getSagaOption() {
    return null;
  }
}
