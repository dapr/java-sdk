package io.dapr.springboot.examples.producer.workflow;

import io.dapr.springboot.examples.producer.Customer;
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
      customer = ctx.callActivity(RegisterCustomerActivity.class.getName(), customer, Customer.class).await();

      customer = ctx.waitForExternalEvent("CustomerReachOut", Duration.ofMinutes(5), Customer.class).await();

      customer = ctx.callActivity(CustomerFollowupActivity.class.getName(), customer, Customer.class).await();

      ctx.complete(customer);
    };
  }
}
