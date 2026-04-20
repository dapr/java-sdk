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
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot4.examples.producer.Customer;
import io.dapr.springboot4.examples.producer.CustomerStore;
import io.dapr.springboot4.examples.producer.OrderDTO;
import io.dapr.springboot4.examples.producer.workflow.CustomerFollowupActivity;
import io.dapr.springboot4.examples.producer.workflow.CustomerWorkflow;
import io.dapr.springboot4.examples.producer.workflow.RegisterCustomerActivity;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {TestProducerApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, CustomerWorkflow.class, CustomerFollowupActivity.class,
        RegisterCustomerActivity.class, CustomerStore.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ProducerAppIT {

  @Autowired
  private TestSubscriberRestController controller;

  @Autowired
  private CustomerStore customerStore;

  @Autowired
  private DaprClient daprClient;

  @Autowired
  private DaprContainer daprContainer;

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    DaprWait.forSubscription("pubsub", "topic").waitUntilReady(daprContainer);
  }

  @AfterEach
  void cleanUp() {
    controller.getAllEvents().clear();
  }

  @Test
  void testOrdersOutboxEndpointAndMessaging() {
    OrderDTO order = new OrderDTO("outbox-order-123", "Lorem ipsum", 1000);

    client.post()
        .uri("/orders/outbox")
        .contentType(MediaType.APPLICATION_JSON)
        .body(order)
        .exchange()
        .expectStatus().isOk();

    await().atMost(Duration.ofSeconds(15))
        .until(controller.getAllEvents()::size, equalTo(1));

  }

//  @Test
//  void testOrdersEndpointAndMessaging() {
//    OrderDTO order = new OrderDTO("abc-123", "the mars volta LP", 1);
//
//    client.post()
//        .uri("/orders")
//        .contentType(MediaType.APPLICATION_JSON)
//        .body(order)
//        .exchange()
//        .expectStatus().isOk();
//
//    await().atMost(Duration.ofSeconds(15))
//            .until(controller.getAllEvents()::size, equalTo(1));
//
//    // Get all orders
//    List<Order> orders = client.get()
//        .uri("/orders")
//        .exchange()
//        .expectStatus().isOk()
//        .returnResult(new ParameterizedTypeReference<List<Order>>() {})
//        .getResponseBody();
//
//    assertNotNull(orders);
//    assertEquals(1, orders.size());
//
//    // Query by item
//    List<Order> ordersByItem = client.get()
//        .uri(uriBuilder -> uriBuilder
//            .path("/orders/byItem/")
//            .queryParam("item", "the mars volta LP")
//            .build())
//        .exchange()
//        .expectStatus().isOk()
//        .returnResult(new ParameterizedTypeReference<List<Order>>() {})
//        .getResponseBody();
//
//    assertNotNull(ordersByItem);
//    assertEquals(1, ordersByItem.size());
//
//    // Query by item - no match
//    List<Order> ordersByOtherItem = client.get()
//        .uri(uriBuilder -> uriBuilder
//            .path("/orders/byItem/")
//            .queryParam("item", "other")
//            .build())
//        .exchange()
//        .expectStatus().isOk()
//        .returnResult(new ParameterizedTypeReference<List<Order>>() {})
//        .getResponseBody();
//
//    assertNotNull(ordersByOtherItem);
//    assertEquals(0, ordersByOtherItem.size());
//
//    // Query by amount
//    List<Order> ordersByAmount = client.get()
//        .uri(uriBuilder -> uriBuilder
//            .path("/orders/byAmount/")
//            .queryParam("amount", 1)
//            .build())
//        .exchange()
//        .expectStatus().isOk()
//        .returnResult(new ParameterizedTypeReference<List<Order>>() {})
//        .getResponseBody();
//
//    assertNotNull(ordersByAmount);
//    assertEquals(1, ordersByAmount.size());
//
//    // Query by amount - no match
//    List<Order> ordersByOtherAmount = client.get()
//        .uri(uriBuilder -> uriBuilder
//            .path("/orders/byAmount/")
//            .queryParam("amount", 2)
//            .build())
//        .exchange()
//        .expectStatus().isOk()
//        .returnResult(new ParameterizedTypeReference<List<Order>>() {})
//        .getResponseBody();
//
//    assertNotNull(ordersByOtherAmount);
//    assertEquals(0, ordersByOtherAmount.size());
//
//  }

  @Test
  void testCustomersWorkflows() {

    client.post()
        .uri("/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customerName\": \"salaboy\"}")
        .exchange()
        .expectStatus().isOk();


    await().atMost(Duration.ofSeconds(15))
            .until(customerStore.getCustomers()::size, equalTo(1));
    Customer customer = customerStore.getCustomer("salaboy");
    assertTrue(customer.isInCustomerDB());
    String workflowId = customer.getWorkflowId();

    client.post()
        .uri("/customers/followup")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{ \"workflowId\": \"" + workflowId + "\",\"customerName\": \"salaboy\" }")
        .exchange()
        .expectStatus().isOk();

    assertEquals(1, customerStore.getCustomers().size());

    await().atMost(Duration.ofSeconds(10))
            .until(customerStore.getCustomer("salaboy")::isFollowUp, equalTo(true));

  }

}
