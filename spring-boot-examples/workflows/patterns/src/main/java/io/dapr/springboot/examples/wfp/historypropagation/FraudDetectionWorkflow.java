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
 * limitations under the License.
 */

package io.dapr.springboot.examples.wfp.historypropagation;

import io.dapr.durabletask.HistoryPropagationScope;
import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.PropagatedHistoryChunk;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Child workflow demonstrating reception of propagated history with LINEAGE scope.
 *
 * <p>This workflow receives the full ancestor history chain from its parent
 * (ProcessPaymentWorkflow). It inspects the history for audit purposes and
 * then calls its own activity with OWN_HISTORY scope.</p>
 */
@Component
public class FraudDetectionWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting FraudDetection workflow: " + ctx.getInstanceId());

      // Access propagated history from parent
      Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();

      if (historyOpt.isPresent()) {
        PropagatedHistory history = historyOpt.get();
        ctx.getLogger().info("Received propagated history with scope: " + history.getScope());
        ctx.getLogger().info("History contains " + history.getEvents().size() + " events");
        ctx.getLogger().info("From apps: " + history.getAppIDs());

        // Verify the parent workflow's card validation happened
        Optional<PropagatedHistoryChunk> parentChunk =
            history.getWorkflowByName("ProcessPaymentWorkflow");
        if (parentChunk.isPresent()) {
          ctx.getLogger().info("Found parent workflow chunk from app: " + parentChunk.get().getAppId()
              + " with " + parentChunk.get().getEventCount() + " events");
        }
      } else {
        ctx.getLogger().info("No propagated history received");
      }

      // Perform fraud check logic
      String input = ctx.getInput(String.class);
      String fraudCheckResult = ctx.callActivity(
          FraudCheckActivity.class.getName(),
          input,
          WorkflowTaskOptions.propagateOwnHistory(),
          String.class
      ).await();

      ctx.complete("fraud-check:" + fraudCheckResult);
    };
  }
}
