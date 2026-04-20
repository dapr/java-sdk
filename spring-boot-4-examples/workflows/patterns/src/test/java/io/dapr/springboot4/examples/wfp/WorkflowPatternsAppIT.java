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

package io.dapr.springboot4.examples.wfp;

import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot4.examples.wfp.continueasnew.CleanUpLog;
import io.dapr.springboot4.examples.wfp.externalevent.Decision;
import io.dapr.springboot4.examples.wfp.fanoutin.Result;
import io.dapr.springboot4.examples.wfp.remoteendpoint.Payload;
import io.dapr.springboot4.examples.wfp.timer.TimerLogService;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class WorkflowPatternsAppIT {

  @Autowired
  private MicrocksContainersEnsemble ensemble;

  @Autowired
  private TimerLogService logService;

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
    logService.clearLog();
  }


  @Test
  void testChainWorkflow() {
    String result = client.post()
            .uri("/wfp/chain")
            .contentType(MediaType.APPLICATION_JSON)
            .body("")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertNotNull(result);
    assertTrue(result.contains("TOKYO, LONDON, SEATTLE"));
  }

  @Test
  void testChildWorkflow() {
    String result = client.post()
            .uri("/wfp/child")
            .contentType(MediaType.APPLICATION_JSON)
            .body("")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertNotNull(result);
    assertTrue(result.contains("!wolfkroW rpaD olleH"));
  }

  @Test
  void testFanOutIn() {
    List<String> listOfStrings = Arrays.asList(
            "Hello, world!",
            "The quick brown fox jumps over the lazy dog.",
            "If a tree falls in the forest and there is no one there to hear it, does it make a sound?",
            "The greatest glory in living lies not in never falling, but in rising every time we fall.",
            "Always remember that you are absolutely unique. Just like everyone else.");

    Result result = client.post()
            .uri("/wfp/fanoutin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(listOfStrings)
            .exchange()
            .expectStatus().isOk()
            .returnResult(Result.class)
            .getResponseBody();

    assertNotNull(result);
    assertEquals(60, result.getWordCount());
  }

  @Test
  void testExternalEventApprove() {

    String instanceId = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/externalevent")
                    .queryParam("orderId", "123")
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertNotNull(instanceId);

    Decision decision = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/externalevent-continue")
                    .queryParam("orderId", "123")
                    .queryParam("decision", true)
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(Decision.class)
            .getResponseBody();

    assertNotNull(decision);
    assertTrue(decision.getApproved());
  }

  @Test
  void testExternalEventDeny() {

    String instanceId = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/externalevent")
                    .queryParam("orderId", "123")
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertNotNull(instanceId);

    Decision decision = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/externalevent-continue")
                    .queryParam("orderId", "123")
                    .queryParam("decision", false)
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(Decision.class)
            .getResponseBody();

    assertNotNull(decision);
    assertEquals(false, decision.getApproved());
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
    CleanUpLog cleanUpLog = client.post()
            .uri("/wfp/continueasnew")
            .contentType(MediaType.APPLICATION_JSON)
            .body("")
            .exchange()
            .expectStatus().isOk()
            .returnResult(CleanUpLog.class)
            .getResponseBody();

    assertNotNull(cleanUpLog);
    assertEquals(5, cleanUpLog.getCleanUpTimes());
  }

  @Test
  void testRemoteEndpoint() {

    Payload payload = client.post()
            .uri("/wfp/remote-endpoint")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new Payload("123", "content goes here"))
            .exchange()
            .expectStatus().isOk()
            .returnResult(Payload.class)
            .getResponseBody();

    assertNotNull(payload);
    assertTrue(payload.getProcessed());

    assertEquals(2, ensemble.getMicrocksContainer()
            .getServiceInvocationsCount("API Payload Processor", "1.0.0"));
  }

  @Test
  void testSuspendResume() {

    String instanceId = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/suspendresume")
                    .queryParam("orderId", "123")
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertNotNull(instanceId);

    // The workflow is waiting on an event, let's suspend the workflow
    String state = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/suspendresume/suspend")
                    .queryParam("orderId", "123")
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertEquals(WorkflowRuntimeStatus.SUSPENDED.name(), state);

    // The let's resume the suspended workflow and check the state
    state = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/suspendresume/resume")
                    .queryParam("orderId", "123")
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

    assertEquals(WorkflowRuntimeStatus.RUNNING.name(), state);

    // Now complete the workflow by sending an event
    Decision decision = client.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/wfp/suspendresume/continue")
                    .queryParam("orderId", "123")
                    .queryParam("decision", false)
                    .build())
            .exchange()
            .expectStatus().isOk()
            .returnResult(Decision.class)
            .getResponseBody();

    assertNotNull(decision);
    assertEquals(false, decision.getApproved());

  }

  @Test
  void testDurationTimer() throws InterruptedException {

    String instanceId = client.post()
            .uri("/wfp/durationtimer")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

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

    String instanceId = client.post()
            .uri("/wfp/zoneddatetimetimer")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class)
            .getResponseBody();

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
