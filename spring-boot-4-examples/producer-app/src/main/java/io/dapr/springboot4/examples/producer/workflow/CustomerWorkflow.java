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
limitations under the License.
*/

package io.dapr.springboot4.examples.producer.workflow;

import io.dapr.springboot4.examples.producer.Customer;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CustomerWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      String instanceId = ctx.getInstanceId();
      Customer customer = ctx.getInput(Customer.class);
      customer.setWorkflowId(instanceId);
      ctx.getLogger().info("Let's register the customer: " + customer.getCustomerName());
      ctx.callActivity(RegisterCustomerActivity.class.getName(), customer, Customer.class).await();
      ctx.getLogger().info("Let's wait for the customer: " + customer.getCustomerName() + " to request a follow up.");
      customer = ctx.waitForExternalEvent("CustomerReachOut", Duration.ofMinutes(5), Customer.class).await();
      ctx.getLogger().info("Let's book a follow up for the customer: " + customer.getCustomerName());
      customer = ctx.callActivity(CustomerFollowupActivity.class.getName(), customer, Customer.class).await();
      ctx.getLogger().info("Congratulations the customer: " + customer.getCustomerName() + " is happy!");
      ctx.complete(customer);
    };
  }
}
