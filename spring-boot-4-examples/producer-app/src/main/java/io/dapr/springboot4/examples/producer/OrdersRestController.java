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

package io.dapr.springboot4.examples.producer;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.spring6.data.repository.config.EnableDaprRepositories;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@EnableDaprRepositories
public class OrdersRestController {

  private static final Logger logger = LoggerFactory.getLogger(OrdersRestController.class);

  @Autowired
  private OrderRepository repository;

  @Autowired
  private DaprMessagingTemplate<Order> messagingTemplate;

  @Autowired
  private DaprClient daprClient;

  /**
   * Store orders from customers.
   * @param order from the customer
   *
   * @return confirmation that the order was stored and the event published
   */
  @PostMapping("/orders")
  public String storeOrder(@RequestBody Order order) {
    logger.info("Storing Order: " + order);
    repository.save(order);
    logger.info("Publishing Order Event: " + order);
    messagingTemplate.send("topic", order);
    return "Order Stored and Event Published";
  }

  @PostMapping("/orders/outbox")
  public String storeOrderOutbox(@RequestBody Order order) {
    logger.info("Storing Order with Outbox: {}", order);
    ExecuteStateTransactionRequest transactionRequest = new ExecuteStateTransactionRequest("kvstore-outbox");

    State<Order> state = new State<>(
        order.getId(), order, null
    );

    TransactionalStateOperation<Order> operation = new TransactionalStateOperation<>(
        TransactionalStateOperation.OperationType.UPSERT, state
    );

    transactionRequest.setOperations(List.of(operation));

    daprClient.executeStateTransaction(transactionRequest).block();

    logger.info("Order Stored with Outbox: {}", order);

    return "Order Stored with Outbox";
  }

  @GetMapping("/orders")
  public Iterable<Order> getAll() {
    return repository.findAll();
  }

  @GetMapping("/orders/byItem/")
  public Iterable<Order> getAllByItem(@RequestParam("item") String item) {
    return repository.findByItem(item);
  }

  @GetMapping("/orders/byAmount/")
  public Iterable<Order> getAllByItem(@RequestParam("amount") Integer amount) {
    return repository.findByAmount(amount);
  }


}

