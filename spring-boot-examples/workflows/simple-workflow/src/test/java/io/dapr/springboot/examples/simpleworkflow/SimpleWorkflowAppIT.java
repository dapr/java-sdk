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

package io.dapr.springboot.examples.simpleworkflow;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {TestSimpleWorkflowApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SimpleWorkflowAppIT {

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
  }

  @Test
  void testCustomerOnboarded() {
    String customerId = UUID.randomUUID().toString();
    startOnboarding(new Customer(customerId, "Ada Lovelace", "ada@example.com"));

    OnboardingStatus status = awaitTerminalStatus(customerId);

    assertEquals(WorkflowRuntimeStatus.COMPLETED, status.getRuntimeStatus());
    assertEquals(customerId, status.getResult().getCustomerId());
    assertEquals("account-" + customerId, status.getResult().getAccountId());
    assertTrue(status.getResult().isWelcomeEmailSent());
    assertNull(status.getError());
  }

  @Test
  void testInvalidCustomerFailsWorkflow() {
    String customerId = UUID.randomUUID().toString();
    startOnboarding(new Customer(customerId, "No Email", null));

    OnboardingStatus status = awaitTerminalStatus(customerId);

    assertEquals(WorkflowRuntimeStatus.FAILED, status.getRuntimeStatus());
    assertNull(status.getResult());
    assertTrue(status.getError().contains("Invalid email for customer " + customerId));
  }

  private void startOnboarding(Customer customer) {
    String instanceId = client.post()
        .uri("/onboarding")
        .contentType(MediaType.APPLICATION_JSON)
        .body(customer)
        .exchange()
        .expectStatus().isOk()
        .returnResult(String.class)
        .getResponseBody();
    assertEquals(customer.getId(), instanceId);
  }

  private OnboardingStatus awaitTerminalStatus(String instanceId) {
    return await().atMost(Duration.ofSeconds(30))
        .until(() -> client.get()
                .uri("/onboarding/" + instanceId)
                .exchange()
                .expectStatus().isOk()
                .returnResult(OnboardingStatus.class)
                .getResponseBody(),
            s -> s.getRuntimeStatus() == WorkflowRuntimeStatus.COMPLETED
                || s.getRuntimeStatus() == WorkflowRuntimeStatus.FAILED);
  }

}
