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

package io.dapr.examples.workflows.historypropagation.multiapp;

import io.dapr.durabletask.PropagatedHistory;
import io.dapr.durabletask.WorkflowResult;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

import java.util.Optional;

/**
 * Activity running in app2 that audits propagated history received from app1.
 *
 * <p>Each contributing workflow's events arrive grouped under a {@link WorkflowResult}
 * tagged with the producing app id, so app2 can verify the chain of custody
 * independently.</p>
 */
public class App2AuditActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    var logger = ctx.getLogger();
    String input = ctx.getInput(String.class);
    logger.info("=== App2 audit activity invoked with input: {} ===", input);

    Optional<PropagatedHistory> historyOpt = ctx.getPropagatedHistory();
    if (historyOpt.isPresent()) {
      PropagatedHistory history = historyOpt.get();
      logger.info("App2 received history (scope={}, workflows={}, apps={})",
          history.getScope(), history.getWorkflows().size(), history.getAppIDs());
      for (WorkflowResult wf : history.getWorkflows()) {
        logger.info("  workflow: app={} name={} instance={}",
            wf.getAppId(), wf.getName(), wf.getInstanceId());
      }
    } else {
      logger.info("App2 received no propagated history");
    }

    return "audited-by-app2:" + input;
  }
}
