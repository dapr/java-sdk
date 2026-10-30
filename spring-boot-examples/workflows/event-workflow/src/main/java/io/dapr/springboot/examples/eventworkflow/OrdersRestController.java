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

import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@EnableDaprWorkflows
public class OrdersRestController {

  private final Logger logger = LoggerFactory.getLogger(OrdersRestController.class);

  @Autowired
  private DaprMessagingTemplate<Order> orderMessagingTemplate;

  @Autowired
  private DaprMessagingTemplate<Payment> paymentMessagingTemplate;

  @Autowired
  private OrderStatusStore orderStatusStore;

  /**
   * Publish an order event. The workflow is started by the subscriber of the orders topic.
   * @param order from the customer
   * @return confirmation that the order event was published
   */
  @PostMapping("/orders")
  public String placeOrder(@RequestBody Order order) {
    if (order.getId() == null) {
      order.setId(UUID.randomUUID().toString());
    }
    logger.info("Publishing Order Event: " + order);
    orderMessagingTemplate.send(EventWorkflowConfiguration.ORDERS_TOPIC, order);
    return "Order Event Published: " + order.getId();
  }

  /**
   * Publish a payment event. The subscriber of the payments topic forwards it to the waiting workflow.
   * @param orderId id of the order being paid
   * @param payment details of the payment
   * @return confirmation that the payment event was published
   */
  @PostMapping("/orders/{orderId}/payment")
  public String pay(@PathVariable("orderId") String orderId, @RequestBody Payment payment) {
    payment.setOrderId(orderId);
    logger.info("Publishing Payment Event: " + payment);
    paymentMessagingTemplate.send(EventWorkflowConfiguration.PAYMENTS_TOPIC, payment);
    return "Payment Event Published: " + orderId;
  }

  /**
   * Get the last status published by the workflow for an order.
   * @param orderId id of the order
   * @return the last known status of the order
   */
  @GetMapping("/orders/{orderId}/status")
  public ResponseEntity<OrderStatus> status(@PathVariable("orderId") String orderId) {
    OrderStatus orderStatus = orderStatusStore.get(orderId);
    if (orderStatus == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(orderStatus);
  }

}
