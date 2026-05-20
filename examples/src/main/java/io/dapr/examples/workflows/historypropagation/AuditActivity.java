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

import io.dapr.durabletask.ActivityResult;
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
      // poking at raw history events.
      for (WorkflowResult wf : history.getWorkflows()) {
        Optional<ActivityResult> validate = wf.getLastActivityByName("ValidateCard");
        validate.ifPresent(activity -> ctx.getLogger().info(
            "  " + wf.getName() + " ran ValidateCard: completed=" + activity.isCompleted()
                + " failed=" + activity.isFailed()
                + (activity.getOutput() != null ? " output=" + activity.getOutput().getValue() : "")));
      }
    } else {
      ctx.getLogger().info("Audit received no propagated history");
    }

    return "audited";
  }
}
