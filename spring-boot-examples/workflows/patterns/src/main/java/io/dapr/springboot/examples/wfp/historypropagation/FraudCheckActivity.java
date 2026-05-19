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

import io.dapr.durabletask.PropagatedHistory;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Activity that performs fraud checks.
 * Receives propagated history with OWN_HISTORY scope from FraudDetectionWorkflow.
 */
@Component
public class FraudCheckActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    ctx.getLogger().info("Running fraud check for: " + ctx.getInput(String.class));

    Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();
    if (historyOpt.isPresent()) {
      PropagatedHistory history = historyOpt.get();
      ctx.getLogger().info("Fraud check has access to propagated history with "
          + history.getEvents().size() + " events (scope: " + history.getScope() + ")");
    }

    return "passed";
  }
}
