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
import io.dapr.springboot.examples.wfp.timer.TimerLogService;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for Dapr Workflow Patterns.
 * 
 * DEBUGGING: For more detailed logs during test execution, you can:
 * 1. Run `docker ps` to find the Dapr container ID
 * 2. Run `docker logs --follow <container-id>` to stream real-time logs
 * 3. The container name will typically be something like "dapr-workflow-patterns-app-<hash>"
 * 
 * Example:
 * ```bash
 * docker ps | grep dapr
 * docker logs --follow <container-id>
 * ```
 * 
 * This will show you detailed Dapr runtime logs including workflow execution,
 * state transitions, and component interactions.
 */
@SpringBootTest(classes = {TestWorkflowPatternsApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, },
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WorkflowPatternsAppTests {

  @Autowired
  private MicrocksContainersEnsemble ensemble;

  @Autowired
  private TimerLogService logService;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8080;
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    logService.clearLog();
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


  /**
   * Tests the ContinueAsNew workflow pattern.
   * 
   * The ContinueAsNew pattern should execute cleanup activities 5 times
   * with 5-second intervals between each iteration.
   */
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

  @Test
  void testDurationTimer() throws InterruptedException {

    String instanceId = given()
            .when()
            .post("/wfp/durationtimer")
            .then()
            .statusCode(200).extract().asString();

    assertNotNull(instanceId);

    // Check that the workflow completed successfully
    await().atMost(Duration.ofSeconds(30))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              System.out.println("Log Size: " + logService.getLogDates().size());
              if( logService.getLogDates().size() == 2 ) {
                long diffInMillis = Math.abs(logService.getLogDates().get(1).getTime() - logService.getLogDates().get(0).getTime());
                long diff = TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                System.out.println("First Log at: " + logService.getLogDates().get(0));
                System.out.println("Second Log at: " + logService.getLogDates().get(1));
                System.out.println("Diff in seconds: " + diff);
                // The updated time differences should be between 9 and 11 seconds
                return diff >= 9 && diff <= 11;
              }
              return false;
            });
  }

  @Test
  void testZonedDateTimeTimer() throws InterruptedException {

    String instanceId = given()
            .when()
            .post("/wfp/zoneddatetimetimer")
            .then()
            .statusCode(200).extract().asString();

    assertNotNull(instanceId);

    // Check that the workflow completed successfully
    await().atMost(Duration.ofSeconds(30))
            .pollDelay(500, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> {
              System.out.println("Log Size: " + logService.getLogDates().size());
              if( logService.getLogDates().size() == 2 ) {
                long diffInMillis = Math.abs(logService.getLogDates().get(1).getTime() - logService.getLogDates().get(0).getTime());
                long diff = TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                System.out.println("First Log at: " + logService.getLogDates().get(0));
                System.out.println("Second Log at: " + logService.getLogDates().get(1));
                System.out.println("Diff in seconds: " + diff);
                // The updated time differences should be between 9 and 11 seconds
                return diff >= 9 && diff <= 11;
              }
              return false;
            });
  }

}
