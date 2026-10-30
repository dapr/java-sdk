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

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {TestEventWorkflowApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class EventWorkflowAppIT {

  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*app is subscribed to the following topics.*";

  @Autowired
  private DaprContainer daprContainer;

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private OrderStatusStore orderStatusStore;

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);
    orderStatusStore.clear();
  }

  @Test
  void testOrderCompletedByPaymentEvent() {
    String orderId = UUID.randomUUID().toString();

    client.post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new Order(orderId, "the mars volta EP", 1))
        .exchange()
        .expectStatus().isOk();

    await().atMost(Duration.ofSeconds(30))
        .until(() -> statusOf(orderId), s -> OrderStatus.WAITING_FOR_PAYMENT.equals(s));

    client.post()
        .uri("/orders/" + orderId + "/payment")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new Payment(orderId, 10.0))
        .exchange()
        .expectStatus().isOk();

    await().atMost(Duration.ofSeconds(30))
        .until(() -> statusOf(orderId), s -> OrderStatus.COMPLETED.equals(s));

    WorkflowState workflowState = daprWorkflowClient.getWorkflowState(orderId, true);
    assertNotNull(workflowState);
    assertEquals(WorkflowRuntimeStatus.COMPLETED, workflowState.getRuntimeStatus());
    assertEquals(OrderStatus.COMPLETED, workflowState.readOutputAs(OrderStatus.class).getStatus());
  }

  private String statusOf(String orderId) {
    OrderStatus orderStatus = orderStatusStore.get(orderId);
    return orderStatus == null ? null : orderStatus.getStatus();
  }

}
