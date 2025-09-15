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

package io.dapr.springboot.examples.workertwo;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerFollowupActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(CustomerFollowupActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Customer customer = ctx.getInput(Customer.class);
    customer.setFollowUp(true);
    logger.info("Customer: {} follow up scheduled.", customer.getCustomerName());
    return customer;
  }

}
