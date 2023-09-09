package io.dapr.examples.workflows.client;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import io.dapr.examples.workflows.console.models.OrderPayload;
import io.dapr.examples.workflows.console.workflows.OrderProcessingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;

public class WorkflowClient {

  public static void main(String[] args) throws InterruptedException {
    DaprWorkflowClient client = new DaprWorkflowClient();
    try (client) {
      testWorkflow(client);
    }
  }

  private static void testWorkflow(DaprWorkflowClient client) {
    // schedule Workflow to order two intel i7 13900KS CPUs
    OrderPayload order = new OrderPayload();
    order.setName("intel-i7-13900KS");
    order.setTotalCost(9000);
    order.setQuantity(2);
    String instanceId = client.scheduleNewWorkflow(OrderProcessingWorkflow.class, order);
    System.out.printf("scheduled new workflow instance of OrderProcessingWorkflow with instance ID: %s%n",
        instanceId);

    try {
      client.waitForInstanceStart(instanceId, Duration.ofSeconds(10), false);
      System.out.printf("workflow instance %s started%n", instanceId);
    } catch (TimeoutException e) {
      System.out.printf("workflow instance %s did not start within 10 seconds%n", instanceId);
      return;
    }

    try {
      WorkflowInstanceStatus workflowStatus = client.waitForInstanceCompletion(instanceId, Duration.ofSeconds(30),
          true);
      if (workflowStatus != null) {
        System.out.printf("workflow instance %s completed, out is: %s %n", instanceId,
            workflowStatus.getSerializedOutput());
      } else {
        System.out.printf("workflow instance %s not found%n", instanceId);
      }
    } catch (TimeoutException e) {
      System.out.printf("workflow instance %s did not complete within 30 seconds%n", instanceId);
    }
  }

}
