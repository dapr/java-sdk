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

package io.dapr.springboot.examples.producer;

import io.dapr.client.DaprClient;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.examples.producer.workflow.CustomerFollowupActivity;
import io.dapr.springboot.examples.producer.workflow.CustomerWorkflow;
import io.dapr.springboot.examples.producer.workflow.RegisterCustomerActivity;
import io.dapr.testcontainers.DaprContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestProducerApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, CustomerWorkflow.class, CustomerFollowupActivity.class,
        RegisterCustomerActivity.class, CustomerStore.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ProducerAppIT {

  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*app is subscribed to the following topics.*";

  @Autowired
  private TestSubscriberRestController controller;

  @Autowired
  private CustomerStore customerStore;

  @Autowired
  private DaprClient daprClient;


  @Autowired
  private DaprContainer daprContainer;


  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    // Ensure the subscriptions are registered
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);

  }

  @AfterEach
  void cleanUp() {
    controller.getAllEvents().clear();
  }

  @Test
  void testOrdersOutboxEndpointAndMessaging() {
    OrderDTO order = new OrderDTO("outbox-order-123", "Lorem ipsum", 1000);

    given().contentType(ContentType.JSON)
        .body(order)
        .when()
        .post("/orders/outbox")
        .then()
        .statusCode(200);

    await().atMost(Duration.ofSeconds(15))
        .until(controller.getAllEvents()::size, equalTo(1));

  }

  @Test
  void testOrdersEndpointAndMessaging() {
    OrderDTO order = new OrderDTO("abc-123", "the mars volta LP", 1);
    given().contentType(ContentType.JSON)
            .body(order)
            .when()
            .post("/orders")
            .then()
            .statusCode(200);

    await().atMost(Duration.ofSeconds(15))
            .until(controller.getAllEvents()::size, equalTo(1));

    given().contentType(ContentType.JSON)
            .when()
            .get("/orders")
            .then()
            .statusCode(200).body("size()", is(1));

    given().contentType(ContentType.JSON)
            .when()
            .queryParam("item", "the mars volta LP")
            .get("/orders/byItem/")
            .then()
            .statusCode(200).body("size()", is(1));

    given().contentType(ContentType.JSON)
            .when()
            .queryParam("item", "other")
            .get("/orders/byItem/")
            .then()
            .statusCode(200).body("size()", is(0));

    given().contentType(ContentType.JSON)
            .when()
            .queryParam("amount", 1)
            .get("/orders/byAmount/")
            .then()
            .statusCode(200).body("size()", is(1));

    given().contentType(ContentType.JSON)
            .when()
            .queryParam("amount", 2)
            .get("/orders/byAmount/")
            .then()
            .statusCode(200).body("size()", is(0));

  }

  @Test
  void testCustomersWorkflows() {

    given().contentType(ContentType.JSON)
            .body("{\"customerName\": \"salaboy\"}")
            .when()
            .post("/customers")
            .then()
            .statusCode(200);


    await().atMost(Duration.ofSeconds(15))
            .until(customerStore.getCustomers()::size, equalTo(1));
    Customer customer = customerStore.getCustomer("salaboy");
    assertEquals(true, customer.isInCustomerDB());
    String workflowId = customer.getWorkflowId();
    given().contentType(ContentType.JSON)
            .body("{ \"workflowId\": \"" + workflowId + "\",\"customerName\": \"salaboy\" }")
            .when()
            .post("/customers/followup")
            .then()
            .statusCode(200);

    assertEquals(1, customerStore.getCustomers().size());

    await().atMost(Duration.ofSeconds(10))
            .until(customerStore.getCustomer("salaboy")::isFollowUp, equalTo(true));

  }

}
