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

package io.dapr.springboot.examples.simpleworkflow;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

/**
 * Onboards a customer by validating it, creating an account and sending a welcome email.
 * Each step is an activity, so a step that already completed is not executed again if the application restarts.
 */
@Component
public class CustomerOnboardingWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());
      Customer customer = ctx.getInput(Customer.class);

      ctx.callActivity(ValidateCustomerActivity.class.getName(), customer, Customer.class).await();
      String accountId = ctx.callActivity(CreateAccountActivity.class.getName(), customer, String.class).await();
      Boolean emailSent = ctx.callActivity(SendWelcomeEmailActivity.class.getName(), customer, Boolean.class).await();

      OnboardingResult result = new OnboardingResult(customer.getId(), accountId, emailSent);
      ctx.getLogger().info("Workflow finished with result: " + result);
      ctx.complete(result);
    };
  }
}
