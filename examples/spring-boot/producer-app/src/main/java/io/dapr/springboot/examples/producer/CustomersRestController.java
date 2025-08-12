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

package io.dapr.springboot.examples.producer;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.examples.producer.workflow.CustomerWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableDaprWorkflows
public class CustomersRestController {


  private final Logger logger = LoggerFactory.getLogger(CustomersRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private CustomerStore customerStore;

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  private Map<String, String> customersWorkflows = new HashMap<>();

  /**
   * Track customer endpoint.
   *
   * @param customer provided customer to track
   * @return confirmation that the workflow instance was created for a given customer
   */
  @PostMapping("/customers")
  public String trackCustomer(@RequestBody Customer customer) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(CustomerWorkflow.class, customer);
    logger.info("Workflow instance " + instanceId + " started");
    customersWorkflows.put(customer.getCustomerName(), instanceId);
    return "New Workflow Instance created for Customer: " + customer.getCustomerName();
  }

  /**
   *  Request customer follow-up.
   *  @param customer associated with a workflow instance
   *  @return confirmation that the follow-up was requested
   */
  @PostMapping("/customers/followup")
  public String customerNotification(@RequestBody Customer customer) {
    logger.info("Customer follow-up requested: " + customer.getCustomerName());
    String workflowIdForCustomer = customersWorkflows.get(customer.getCustomerName());
    if (workflowIdForCustomer == null || workflowIdForCustomer.isEmpty()) {
      return "There is no workflow associated with customer: " + customer.getCustomerName();
    } else {
      daprWorkflowClient.raiseEvent(workflowIdForCustomer, "CustomerReachOut", customer);
      return "Customer Follow-up requested";
    }
  }


  public Collection<Customer> getCustomers() {
    return customerStore.getCustomers();
  }


}

