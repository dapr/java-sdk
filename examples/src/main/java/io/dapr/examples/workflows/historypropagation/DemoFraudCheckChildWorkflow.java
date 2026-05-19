/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.examples.workflows.historypropagation;

import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.PropagatedHistoryChunk;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import java.util.Optional;

/**
 * Child workflow that inspects the propagated history it received from its parent.
 *
 * <p>Because the parent invoked this workflow with LINEAGE scope, the child can see
 * the full ancestor history chain via {@code ctx.getPropagatedHistory()}.</p>
 */
public class DemoFraudCheckChildWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting child workflow: " + ctx.getInstanceId());

      Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();
      if (historyOpt.isPresent()) {
        PropagatedHistory history = historyOpt.get();
        ctx.getLogger().info("Received propagated history (scope=" + history.getScope()
            + ", events=" + history.getEvents().size() + ")");
        for (PropagatedHistoryChunk chunk : history.getWorkflows()) {
          ctx.getLogger().info("  chunk: workflow=" + chunk.getWorkflowName()
              + " app=" + chunk.getAppId() + " events=" + chunk.getEventCount());
        }
      } else {
        ctx.getLogger().info("No propagated history received");
      }

      ctx.complete("fraud-check-passed");
    };
  }
}
