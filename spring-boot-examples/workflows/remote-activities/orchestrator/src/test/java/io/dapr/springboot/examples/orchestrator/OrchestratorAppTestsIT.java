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

package io.dapr.springboot.examples.orchestrator;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestOrchestratorApplication.class, DaprTestContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"reuse=false", "tests.workers.enabled=true"})
class OrchestratorAppTestsIT {


  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);

  }

  @Test
  void testCustomersWorkflows() throws InterruptedException, IOException {

    given().contentType(ContentType.JSON)
            .body("{\"customerName\": \"salaboy\"}")
            .when()
            .post("/customers")
            .then()
            .statusCode(200);


//    await().atMost(Duration.ofSeconds(15))
//            .until(customerStore.getCustomers()::size, equalTo(1));
//    io.dapr.springboot.examples.orchestrator.Customer customer = customerStore.getCustomer("salaboy");
//    assertEquals(true, customer.isInCustomerDB());

//    String workflowId = customer.getWorkflowId();

    Thread.sleep(1000);

    given().contentType(ContentType.JSON)
            .body("{\"customerName\": \"salaboy\" }")
            .when()
            .post("/customers/followup")
            .then()
            .statusCode(200);

    Thread.sleep(1000);

    given().contentType(ContentType.JSON)
            .body("{\"customerName\": \"salaboy\" }")
            .when()
            .post("/customers/output")
            .then()
            .statusCode(200);
//  }
//
//    assertEquals(1, customerStore.getCustomers().size());
//
//    await().atMost(Duration.ofSeconds(10))
//            .until(customerStore.getCustomer("salaboy")::isFollowUp, equalTo(true));

  }

}
