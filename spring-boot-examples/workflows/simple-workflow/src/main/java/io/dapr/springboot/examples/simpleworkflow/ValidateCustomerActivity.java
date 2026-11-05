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

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidateCustomerActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(ValidateCustomerActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    logger.info("Starting Activity: " + ctx.getName());
    Customer customer = ctx.getInput(Customer.class);
    if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
      // An exception thrown by an activity fails the workflow unless the workflow catches it
      throw new IllegalArgumentException("Invalid email for customer " + customer.getId());
    }
    logger.info("Customer " + customer.getId() + " is valid");
    return customer;
  }
}
