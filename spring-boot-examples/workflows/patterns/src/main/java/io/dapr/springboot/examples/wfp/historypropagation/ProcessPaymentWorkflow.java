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
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import org.springframework.stereotype.Component;

/**
 * Parent workflow demonstrating history propagation.
 *
 * <p>This workflow processes a payment and propagates its execution history to downstream
 * workflows and activities for audit, fraud detection, and chain-of-custody verification.</p>
 *
 * <p>Flow: ProcessPayment → ValidateCard (activity) → FraudDetection (child wf with LINEAGE)
 * → SettlePayment (activity with OWN_HISTORY)</p>
 */
@Component
public class ProcessPaymentWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting ProcessPayment workflow: " + ctx.getInstanceId());

      String paymentInput = ctx.getInput(String.class);

      // Step 1: Validate the card (no propagation needed for this step)
      String validationResult = ctx.callActivity(
          ValidateCardActivity.class.getName(),
          paymentInput,
          String.class
      ).await();
      ctx.getLogger().info("Card validation result: " + validationResult);

      // Step 2: Call fraud detection child workflow with LINEAGE propagation.
      // The child will receive this workflow's full execution history (including
      // the ValidateCard activity above) plus any ancestor history.
      String fraudResult = ctx.callChildWorkflow(
          FraudDetectionWorkflow.class.getName(),
          paymentInput,
          null,
          WorkflowTaskOptions.propagateLineage(),
          String.class
      ).await();
      ctx.getLogger().info("Fraud detection result: " + fraudResult);

      // Step 3: Settle the payment with OWN_HISTORY propagation.
      // The activity receives only this workflow's events (trust boundary -
      // no grandparent history if this workflow was itself called with LINEAGE).
      String settlementResult = ctx.callActivity(
          SettlePaymentActivity.class.getName(),
          paymentInput,
          WorkflowTaskOptions.propagateOwnHistory(),
          String.class
      ).await();
      ctx.getLogger().info("Settlement result: " + settlementResult);

      ctx.complete("Payment processed: " + settlementResult);
    };
  }
}
