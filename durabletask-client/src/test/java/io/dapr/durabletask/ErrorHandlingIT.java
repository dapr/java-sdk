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

package io.dapr.durabletask;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These integration tests are designed to exercise the core, high-level error-handling features of the Durable Task
 * programming model.
 * <p/>
 * These tests currently require a sidecar process to be running on the local machine (the sidecar is what accepts the
 * client operations and sends invocation instructions to the DurableTaskWorker).
 */
@Tag("integration")
public class ErrorHandlingIT extends IntegrationTestBase {
  @Test
  void orchestratorException() throws TimeoutException {
    final String orchestratorName = "OrchestratorWithException";
    final String errorMessage = "Kah-BOOOOOM!!!";

    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> {
              throw new RuntimeException(errorMessage);
            })
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, 0);
      OrchestrationMetadata instance = client.waitForInstanceCompletion(instanceId, defaultTimeout, true);
      assertNotNull(instance);
      assertEquals(OrchestrationRuntimeStatus.FAILED, instance.getRuntimeStatus());

      FailureDetails details = instance.getFailureDetails();
      assertNotNull(details);
      assertEquals("java.lang.RuntimeException", details.getErrorType());
      assertTrue(details.getErrorMessage().contains(errorMessage));
      assertNotNull(details.getStackTrace());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void activityException(boolean handleException) throws TimeoutException {
    final String orchestratorName = "OrchestratorWithActivityException";
    final String activityName = "Throw";
    final String errorMessage = "Kah-BOOOOOM!!!";

    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> {
              try {
                ctx.callActivity(activityName).await();
              } catch (TaskFailedException ex) {
                if (handleException) {
                  ctx.complete("handled");
                } else {
                  throw ex;
                }
              }
            })
            .addActivity(activityName, ctx -> {
              throw new RuntimeException(errorMessage);
            })
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, "");
      OrchestrationMetadata instance = client.waitForInstanceCompletion(instanceId, defaultTimeout, true);
      assertNotNull(instance);

      if (handleException) {
        String result = instance.readOutputAs(String.class);
        assertNotNull(result);
        assertEquals("handled", result);
      } else {
        assertEquals(OrchestrationRuntimeStatus.FAILED, instance.getRuntimeStatus());

        FailureDetails details = instance.getFailureDetails();
        assertNotNull(details);

        String expectedMessage = String.format(
                "Task '%s' (#0) failed with an unhandled exception: %s",
                activityName,
                errorMessage);
        assertEquals(expectedMessage, details.getErrorMessage());
        assertEquals("io.dapr.durabletask.TaskFailedException", details.getErrorType());
        assertNotNull(details.getStackTrace());
        // CONSIDER: Additional validation of getErrorDetails?
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  public void retryActivityFailures(int maxNumberOfAttempts) throws TimeoutException {
    // There is one task for each activity call and one task between each retry
    int expectedTaskCount = (maxNumberOfAttempts * 2) - 1;
    this.retryOnFailuresCoreTest(maxNumberOfAttempts, expectedTaskCount, ctx -> {
      RetryPolicy retryPolicy = getCommonRetryPolicy(maxNumberOfAttempts);
      ctx.callActivity(
              "BustedActivity",
              null,
              TaskOptions.withRetryPolicy(retryPolicy)).await();
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  public void retryActivityFailuresWithCustomLogic(int maxNumberOfAttempts) throws TimeoutException {
    // This gets incremented every time the retry handler is invoked
    AtomicInteger retryHandlerCalls = new AtomicInteger();

    // Run the test and get back the details of the last failure
    this.retryOnFailuresCoreTest(maxNumberOfAttempts, maxNumberOfAttempts, ctx -> {
      RetryHandler retryHandler = getCommonRetryHandler(retryHandlerCalls, maxNumberOfAttempts);
      TaskOptions options = TaskOptions.withRetryHandler(retryHandler);
      ctx.callActivity("BustedActivity", null, options).await();
    });

    // Assert that the retry handle got invoked the expected number of times
    assertEquals(maxNumberOfAttempts, retryHandlerCalls.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void subOrchestrationException(boolean handleException) throws TimeoutException {
    final String orchestratorName = "OrchestrationWithBustedSubOrchestrator";
    final String subOrchestratorName = "BustedSubOrchestrator";
    final String errorMessage = "Kah-BOOOOOM!!!";

    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, ctx -> {
              try {
                String result = ctx.callSubOrchestrator(subOrchestratorName, "", String.class).await();
                ctx.complete(result);
              } catch (TaskFailedException ex) {
                if (handleException) {
                  ctx.complete("handled");
                } else {
                  throw ex;
                }
              }
            })
            .addOrchestrator(subOrchestratorName, ctx -> {
              throw new RuntimeException(errorMessage);
            })
            .buildAndStart();
    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, 1);
      OrchestrationMetadata instance = client.waitForInstanceCompletion(instanceId, defaultTimeout, true);
      assertNotNull(instance);
      if (handleException) {
        assertEquals(OrchestrationRuntimeStatus.COMPLETED, instance.getRuntimeStatus());
        String result = instance.readOutputAs(String.class);
        assertNotNull(result);
        assertEquals("handled", result);
      } else {
        assertEquals(OrchestrationRuntimeStatus.FAILED, instance.getRuntimeStatus());
        FailureDetails details = instance.getFailureDetails();
        assertNotNull(details);
        String expectedMessage = String.format(
                "Task '%s' (#0) failed with an unhandled exception: %s",
                subOrchestratorName,
                errorMessage);
        assertEquals(expectedMessage, details.getErrorMessage());
        assertEquals("io.dapr.durabletask.TaskFailedException", details.getErrorType());
        assertNotNull(details.getStackTrace());
        // CONSIDER: Additional validation of getStackTrace?
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  public void retrySubOrchestratorFailures(int maxNumberOfAttempts) throws TimeoutException {
    // There is one task for each sub-orchestrator call and one task between each retry
    int expectedTaskCount = (maxNumberOfAttempts * 2) - 1;
    this.retryOnFailuresCoreTest(maxNumberOfAttempts, expectedTaskCount, ctx -> {
      RetryPolicy retryPolicy = getCommonRetryPolicy(maxNumberOfAttempts);
      ctx.callSubOrchestrator(
              "BustedSubOrchestrator",
              null,
              null,
              TaskOptions.withRetryPolicy(retryPolicy)).await();
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 10})
  public void retrySubOrchestrationFailuresWithCustomLogic(int maxNumberOfAttempts) throws TimeoutException {
    // This gets incremented every time the retry handler is invoked
    AtomicInteger retryHandlerCalls = new AtomicInteger();

    // Run the test and get back the details of the last failure
    this.retryOnFailuresCoreTest(maxNumberOfAttempts, maxNumberOfAttempts, ctx -> {
      RetryHandler retryHandler = getCommonRetryHandler(retryHandlerCalls, maxNumberOfAttempts);
      TaskOptions options = TaskOptions.withRetryHandler(retryHandler);
      ctx.callSubOrchestrator("BustedSubOrchestrator", null, null, options).await();
    });

    // Assert that the retry handle got invoked the expected number of times
    assertEquals(maxNumberOfAttempts, retryHandlerCalls.get());
  }

  private static RetryPolicy getCommonRetryPolicy(int maxNumberOfAttempts) {
    // Include a small delay between each retry to exercise the implicit timer path
    return new RetryPolicy(maxNumberOfAttempts, Duration.ofMillis(1));
  }

  private static RetryHandler getCommonRetryHandler(AtomicInteger handlerInvocationCounter, int maxNumberOfAttempts) {
    return ctx -> {
      // Retry handlers get executed on the orchestrator thread and go through replay
      if (!ctx.getOrchestrationContext().getIsReplaying()) {
        handlerInvocationCounter.getAndIncrement();
      }

      // The isCausedBy() method is designed to handle exception inheritance
      if (!ctx.getLastFailure().isCausedBy(Exception.class)) {
        return false;
      }

      // This is the actual exception type we care about
      if (!ctx.getLastFailure().isCausedBy(RuntimeException.class)) {
        return false;
      }

      // Quit after N attempts
      return ctx.getLastAttemptNumber() < maxNumberOfAttempts;
    };
  }

  /**
   * Shared logic for execution an orchestration with an activity that constantly fails.
   *
   * @param maxNumberOfAttempts The expected maximum number of activity execution attempts
   * @param expectedTaskCount   The expected number of tasks to be scheduled by the main orchestration.
   * @param mainOrchestration   The main orchestration implementation, which is expected to call either the
   *                            "BustedActivity" activity or the "BustedSubOrchestrator" sub-orchestration.
   * @return Returns the details of the <i>last</i> activity or sub-orchestration failure.
   */
  private FailureDetails retryOnFailuresCoreTest(
          int maxNumberOfAttempts,
          int expectedTaskCount,
          TaskOrchestration mainOrchestration) throws TimeoutException {
    final String orchestratorName = "MainOrchestrator";

    AtomicInteger actualAttemptCount = new AtomicInteger();

    // The caller of this test provides the top-level orchestration implementation. This method provides both a
    // failing sub-orchestration and a failing activity implementation for it to use. The expectation is that the
    // main orchestration tries to invoke just one of them and is configured with retry configuration.
    AtomicBoolean isActivityPath = new AtomicBoolean(false);
    DurableTaskGrpcWorker worker = this.createWorkerBuilder()
            .addOrchestrator(orchestratorName, mainOrchestration)
            .addOrchestrator("BustedSubOrchestrator", ctx -> {
              actualAttemptCount.getAndIncrement();
              throw new RuntimeException("Error #" + actualAttemptCount.get());
            })
            .addActivity("BustedActivity", ctx -> {
              actualAttemptCount.getAndIncrement();
              isActivityPath.set(true);
              throw new RuntimeException("Error #" + actualAttemptCount.get());
            })
            .buildAndStart();

    DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
    try (worker; client) {
      String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, "");
      OrchestrationMetadata instance = client.waitForInstanceCompletion(instanceId, defaultTimeout, true);
      assertNotNull(instance);
      assertEquals(OrchestrationRuntimeStatus.FAILED, instance.getRuntimeStatus());

      // Make sure the exception details are still what we expect
      FailureDetails details = instance.getFailureDetails();
      assertNotNull(details);

      // Confirm the number of attempts
      assertEquals(maxNumberOfAttempts, actualAttemptCount.get());

      return details;
    }
  }
}