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

package io.dapr.springboot.examples.wfp;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.examples.wfp.continueasnew.CleanUpLog;
import io.dapr.springboot.examples.wfp.remoteendpoint.Payload;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {TestWorkflowPatternsApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, },
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WorkflowPatternsAppTests {

  @Autowired
  private MicrocksContainersEnsemble ensemble;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }


  @Test
  void testChainWorkflow() {
    given().contentType(ContentType.JSON)
            .body("")
            .when()
            .post("/wfp/chain")
            .then()
            .statusCode(200).body(containsString("TOKYO, LONDON, SEATTLE"));
  }

  @Test
  void testChildWorkflow() {
    given().contentType(ContentType.JSON)
            .body("")
            .when()
            .post("/wfp/child")
            .then()
            .statusCode(200).body(containsString("!wolfkroW rpaD olleH"));
  }

  @Test
  void testFanOutIn() {
    List<String> listOfStrings = Arrays.asList(
            "Hello, world!",
            "The quick brown fox jumps over the lazy dog.",
            "If a tree falls in the forest and there is no one there to hear it, does it make a sound?",
            "The greatest glory in living lies not in never falling, but in rising every time we fall.",
            "Always remember that you are absolutely unique. Just like everyone else.");

    given().contentType(ContentType.JSON)
            .body(listOfStrings)
            .when()
            .post("/wfp/fanoutin")
            .then()
            .statusCode(200).body("wordCount",equalTo(60));
  }

  @Test
  void testExternalEventApprove() {

    given()
            .queryParam("orderId", "123")
            .when()
            .post("/wfp/externalevent")
            .then()
            .statusCode(200).extract().asString();



    given()
            .queryParam("orderId", "123")
            .queryParam("decision", true)
            .when()
            .post("/wfp/externalevent-continue")
            .then()
            .statusCode(200).body("approved", equalTo(true));
  }

  @Test
  void testExternalEventDeny() {

    given()
            .queryParam("orderId", "123")
            .when()
            .post("/wfp/externalevent")
            .then()
            .statusCode(200).extract().asString();



    given()
            .queryParam("orderId", "123")
            .queryParam("decision", false)
            .when()
            .post("/wfp/externalevent-continue")
            .then()
            .statusCode(200).body("approved", equalTo(false));
  }


  @Test
  void testContinueAsNew() {
    //This call blocks until all the clean up activities are executed
    CleanUpLog cleanUpLog = given().contentType(ContentType.JSON)
            .body("")
            .when()
            .post("/wfp/continueasnew")
            .then()
            .statusCode(200).extract().as(CleanUpLog.class);

    assertEquals(5, cleanUpLog.getCleanUpTimes());
  }

  @Test
  void testRemoteEndpoint() {

    Payload payload = given().contentType(ContentType.JSON)
            .body(new Payload("123", "content goes here"))
            .when()
            .post("/wfp/remote-endpoint")
            .then()
            .statusCode(200).extract().as(Payload.class);

    assertEquals(true, payload.getProcessed());

    assertEquals(2, ensemble.getMicrocksContainer()
            .getServiceInvocationsCount("API Payload Processor", "1.0.0"));
  }

  @Test
  void testSuspendResume() {

    String instanceId = given()
            .queryParam("orderId", "123")
            .when()
            .post("/wfp/suspendresume")
            .then()
            .statusCode(200).extract().asString();

    assertNotNull(instanceId);

    // The workflow is waiting on an event, let's suspend the workflow
    String state = given()
            .queryParam("orderId", "123")
            .when()
            .post("/wfp/suspendresume/suspend")
            .then()
            .statusCode(200).extract().asString();

    assertEquals(WorkflowRuntimeStatus.SUSPENDED.name(), state);

    // The let's resume the suspended workflow and check the state
    state = given()
            .queryParam("orderId", "123")
            .when()
            .post("/wfp/suspendresume/resume")
            .then()
            .statusCode(200).extract().asString();

    assertEquals(WorkflowRuntimeStatus.RUNNING.name(), state);

    // Now complete the workflow by sending an event
    given()
            .queryParam("orderId", "123")
            .queryParam("decision", false)
            .when()
            .post("/wfp/suspendresume/continue")
            .then()
            .statusCode(200).body("approved", equalTo(false));

  }

}
