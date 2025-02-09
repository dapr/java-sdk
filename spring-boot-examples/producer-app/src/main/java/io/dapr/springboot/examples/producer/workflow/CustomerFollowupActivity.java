package io.dapr.springboot.examples.producer.workflow;

import io.dapr.springboot.examples.producer.Customer;
import io.dapr.springboot.examples.producer.CustomerStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.stereotype.Component;

@Component
public class CustomerFollowupActivity implements WorkflowActivity {

  private final CustomerStore customerStore;

  public CustomerFollowupActivity(CustomerStore customerStore) {
    this.customerStore = customerStore;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Customer customer = ctx.getInput(Customer.class);
    //Let's get the hydrate the real customer from the CustomerStore
    customer = customerStore.getCustomer(customer.getCustomerName());
    customer.setFollowUp(true);
    customerStore.addCustomer(customer);
    System.out.println("Customer: " + customer + " follow-up." + " - hashcode" + this.hashCode());
    return customer;
  }

}
