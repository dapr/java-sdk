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

package io.dapr.springboot.examples.eventworkflow;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscribes to the Pub/Sub topics that drive the {@link OrderProcessingWorkflow}.
 */
@RestController
public class OrderEventsSubscriber {

  private final Logger logger = LoggerFactory.getLogger(OrderEventsSubscriber.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private OrderStatusStore orderStatusStore;

  /**
   * Start a workflow instance for each order event. The order id is used as the workflow instance id,
   * so a redelivered event does not start a second instance for the same order.
   * @param cloudEvent order event
   */
  @PostMapping("events/orders")
  @Topic(pubsubName = "pubsub", name = EventWorkflowConfiguration.ORDERS_TOPIC)
  public void onOrder(@RequestBody CloudEvent<Order> cloudEvent) {
    Order order = cloudEvent.getData();
    logger.info("Order Event Received: " + order);
    try {
      daprWorkflowClient.scheduleNewWorkflow(OrderProcessingWorkflow.class, order, order.getId());
      logger.info("Workflow instance " + order.getId() + " started");
    } catch (WorkflowInstanceAlreadyExistsException e) {
      logger.info("Workflow instance " + order.getId() + " already exists, ignoring duplicated event");
    }
  }

  /**
   * Forward each payment event to the workflow instance waiting for it.
   * @param cloudEvent payment event
   */
  @PostMapping("events/payments")
  @Topic(pubsubName = "pubsub", name = EventWorkflowConfiguration.PAYMENTS_TOPIC)
  public void onPayment(@RequestBody CloudEvent<Payment> cloudEvent) {
    Payment payment = cloudEvent.getData();
    logger.info("Payment Event Received: " + payment);
    daprWorkflowClient.raiseEvent(payment.getOrderId(), OrderProcessingWorkflow.PAYMENT_RECEIVED_EVENT, payment);
  }

  /**
   * Record the status events published by the workflow.
   * @param cloudEvent order status event
   */
  @PostMapping("events/order-status")
  @Topic(pubsubName = "pubsub", name = EventWorkflowConfiguration.ORDER_STATUS_TOPIC)
  public void onOrderStatus(@RequestBody CloudEvent<OrderStatus> cloudEvent) {
    OrderStatus orderStatus = cloudEvent.getData();
    logger.info("Order Status Event Received: " + orderStatus);
    orderStatusStore.put(orderStatus);
  }

}
