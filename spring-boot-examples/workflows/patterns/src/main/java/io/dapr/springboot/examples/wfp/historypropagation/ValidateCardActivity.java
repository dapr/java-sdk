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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.stereotype.Component;

/**
 * Activity that validates a payment card.
 * This activity does not receive propagated history (called without propagation scope).
 */
@Component
public class ValidateCardActivity implements WorkflowActivity {
  @Override
  public Object run(WorkflowActivityContext ctx) {
    ctx.getLogger().info("Validating card for: " + ctx.getInput(String.class));
    return "card-valid";
  }
}
