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

import io.dapr.durabletask.ChildWorkflowResult;
import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.WorkflowResult;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

import java.util.Optional;

/**
 * Activity that audits its caller's execution history.
 *
 * <p>Invoked with OWN_HISTORY scope - sees only the immediate caller's events
 * (a trust boundary; no grandparent history is propagated through).</p>
 */
public class AuditActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    ctx.getLogger().info("Auditing payment for: " + ctx.getInput(String.class));

    Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();
    if (historyOpt.isPresent()) {
      PropagatedHistory history = historyOpt.get();
      ctx.getLogger().info("Audit received history (scope=" + history.getScope()
          + ", workflows=" + history.getWorkflows().size() + ")");

      // Surface what the immediate caller did, using typed lookups instead of
      // poking at raw history events. The parent of this activity is
      // DemoHistoryPropagationWorkflow, which invokes DemoFraudCheckChildWorkflow.
      for (WorkflowResult wf : history.getWorkflows()) {
        Optional<ChildWorkflowResult> child =
            wf.getLastChildWorkflowByName(DemoFraudCheckChildWorkflow.class.getName());
        child.ifPresent(c -> ctx.getLogger().info(
            "  " + wf.getName() + " ran child " + c.getName()
                + ": completed=" + c.isCompleted()
                + " failed=" + c.isFailed()
                + (c.getOutput() != null ? " output=" + c.getOutput().getValue() : "")));
      }
    } else {
      ctx.getLogger().info("Audit received no propagated history");
    }

    return "audited";
  }
}
