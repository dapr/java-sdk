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

import io.dapr.durabletask.ActivityResult;
import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.WorkflowResult;
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

      Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();

      if (historyOpt.isPresent()) {
        PropagatedHistory history = historyOpt.get();
        ctx.getLogger().info("Received propagated history with scope: " + history.getScope());
        ctx.getLogger().info("From apps: " + history.getAppIDs());

        // Verify the parent workflow's card validation happened, using the
        // typed activity lookup rather than walking raw events.
        Optional<WorkflowResult> parent =
            history.getLastWorkflowByName(ProcessPaymentWorkflow.class.getName());
        if (parent.isPresent()) {
          ctx.getLogger().info("Found parent workflow from app: " + parent.get().getAppId());
          Optional<ActivityResult> validate =
              parent.get().getLastActivityByName(ValidateCardActivity.class.getName());
          validate.ifPresent(activity ->
              ctx.getLogger().info("  ValidateCard: completed=" + activity.isCompleted()
                  + " failed=" + activity.isFailed()));
        }
      } else {
        ctx.getLogger().info("No propagated history received");
      }

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
