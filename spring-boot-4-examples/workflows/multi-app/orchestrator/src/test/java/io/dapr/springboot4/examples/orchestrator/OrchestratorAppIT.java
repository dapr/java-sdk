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

package io.dapr.springboot4.examples.orchestrator;

import io.dapr.springboot.examples.orchestrator.Customer;
import io.dapr.springboot.examples.orchestrator.CustomersRestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {TestOrchestratorApplication.class, DaprTestContainersConfig.class, CustomersRestController.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"reuse=false", "tests.workers.enabled=true"})
class OrchestratorAppIT {

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    org.testcontainers.Testcontainers.exposeHostPorts(8080);

  }

  @Test
  void testCustomersWorkflows() throws InterruptedException {

    // Create a new workflow instance for a given customer
    client.post()
        .uri("/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customerName\": \"salaboy\"}")
        .exchange()
        .expectStatus().isOk();

    // Wait for the workflow instance to be running by checking the status
    await().atMost(Duration.ofSeconds(5)).until(() ->
            {
              String workflowStatus = client.post()
                      .uri("/customers/status")
                      .contentType(MediaType.APPLICATION_JSON)
                      .body("{\"customerName\": \"salaboy\" }")
                      .exchange()
                      .expectStatus().isOk()
                      .returnResult(String.class)
                      .getResponseBody();
              return "Workflow for Customer: salaboy is RUNNING".equals(workflowStatus);
            }
    );

    // Raise an external event to move the workflow forward
    client.post()
        .uri("/customers/followup")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customerName\": \"salaboy\" }")
        .exchange()
        .expectStatus().isOk();

    // Wait for the workflow instance to be completed by checking the status
    await().atMost(Duration.ofSeconds(5)).until(() ->
            {
              String workflowStatus = client.post()
                      .uri("/customers/status")
                      .contentType(MediaType.APPLICATION_JSON)
                      .body("{\"customerName\": \"salaboy\" }")
                      .exchange()
                      .expectStatus().isOk()
                      .returnResult(String.class)
                      .getResponseBody();
              return "Workflow for Customer: salaboy is COMPLETED".equals(workflowStatus);
            }
    );

    // Get the customer after running all the workflow activities
    Customer customer = client.post()
        .uri("/customers/output")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customerName\": \"salaboy\" }")
        .exchange()
        .expectStatus().isOk()
        .returnResult(Customer.class)
        .getResponseBody();

    assertNotNull(customer);
    assertTrue(customer.isInCustomerDB());
    assertTrue(customer.isFollowUp());


  }

}
