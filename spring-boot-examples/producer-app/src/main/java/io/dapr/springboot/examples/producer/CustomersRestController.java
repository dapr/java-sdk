package io.dapr.springboot.examples.producer;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import io.dapr.springboot.examples.producer.workflow.CustomerWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@EnableDaprWorkflows
public class CustomersRestController {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private CustomerStore customerStore;

  @GetMapping("/")
  public String root() {
    return "OK";
  }

  /**
   * Track customer endpoint.
   * @param customer provided customer to track
   * @return workflowId
   */
  @PostMapping("/customers")
  public String trackCustomer(@RequestBody Customer customer) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(CustomerWorkflow.class, customer);
    System.out.printf("Workflow instance %s started%n", instanceId);
    return instanceId;
  }

  @PostMapping("/customers/followup")
  public void customerNotification(@RequestBody Customer customer) {
    daprWorkflowClient.raiseEvent(customer.getWorkflowId(), "CustomerReachOut", customer);
  }


  public Collection<Customer> getCustomers() {
    return customerStore.getCustomers();
  }


}

