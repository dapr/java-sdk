/*
 * Copyright 2025 The Dapr Authors
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

import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.PropagatedHistoryChunk;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Activity that settles a payment.
 * Receives propagated history with OWN_HISTORY scope — only sees the immediate
 * caller's events (trust boundary; grandparent history is dropped).
 */
@Component
public class SettlePaymentActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    ctx.getLogger().info("Settling payment for: " + ctx.getInput(String.class));

    Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();
    if (historyOpt.isPresent()) {
      PropagatedHistory history = historyOpt.get();
      ctx.getLogger().info("Settlement activity has propagated history (scope: "
          + history.getScope() + ")");
      ctx.getLogger().info("Events from " + history.getWorkflows().size() + " workflow(s):");
      for (PropagatedHistoryChunk chunk : history.getWorkflows()) {
        ctx.getLogger().info("  - " + chunk.getWorkflowName() + " (" + chunk.getAppId()
            + "): " + chunk.getEventCount() + " events");
      }
    }

    return "settled";
  }
}
