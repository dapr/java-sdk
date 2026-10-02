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

package io.dapr.workflows.task.internal;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import io.dapr.workflows.task.Task;
import io.dapr.workflows.task.TaskOrchestration;
import io.dapr.workflows.task.exception.CompositeTaskFailedException;
import io.dapr.workflows.task.orchestration.TaskOrchestrationFactories;
import io.dapr.workflows.task.orchestration.TaskOrchestrationFactory;
import io.dapr.workflows.task.serialization.JacksonDataConverter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives orchestrations through the replay executor to cover the workflow context operations:
 * fanning tasks in and out, sending events to other instances, patching, and the argument
 * checks each operation makes.
 */
class WorkflowContextBehaviourTest {

  private static final Logger logger = Logger.getLogger(WorkflowContextBehaviourTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);
  private static final Instant TEST_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
  private static final String INSTANCE_ID = "instance-1";
  private static final String ORCHESTRATOR = "DemoWorkflow";

  private static Timestamp ts(Instant instant) {
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  private static HistoryEvents.HistoryEvent workflowStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
  }

  private static HistoryEvents.HistoryEvent executionStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(ORCHESTRATOR)
            .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId(INSTANCE_ID)))
        .build();
  }

  private static HistoryEvents.HistoryEvent taskScheduled(int eventId, String name) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName(name))
        .build();
  }

  private static HistoryEvents.HistoryEvent taskCompleted(int taskScheduledId, String result) {
    HistoryEvents.TaskCompletedEvent.Builder completed =
        HistoryEvents.TaskCompletedEvent.newBuilder().setTaskScheduledId(taskScheduledId);
    if (result != null) {
      completed.setResult(StringValue.of(result));
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskCompleted(completed)
        .build();
  }

  private static HistoryEvents.HistoryEvent taskFailed(int taskScheduledId, String errorMessage) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskFailed(HistoryEvents.TaskFailedEvent.newBuilder()
            .setTaskScheduledId(taskScheduledId)
            .setFailureDetails(Orchestration.TaskFailureDetails.newBuilder()
                .setErrorType("TestError")
                .setErrorMessage(errorMessage)))
        .build();
  }

  private static TaskOrchestrationExecutor executor(TaskOrchestration orchestration) {
    TaskOrchestrationFactories factories = new TaskOrchestrationFactories();
    factories.addOrchestration(new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return ORCHESTRATOR;
      }

      @Override
      public TaskOrchestration create() {
        return orchestration;
      }

      @Override
      public String getVersionName() {
        return "";
      }

      @Override
      public Boolean isLatestVersion() {
        return false;
      }
    });

    return new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, logger, null);
  }

  private static TaskOrchestratorResult run(TaskOrchestration orchestration,
                                            List<HistoryEvents.HistoryEvent> past,
                                            List<HistoryEvents.HistoryEvent> newEvents) {
    return executor(orchestration).execute(past, newEvents);
  }

  private static TaskOrchestratorResult runFromStart(TaskOrchestration orchestration) {
    return run(orchestration, Collections.emptyList(), Arrays.asList(workflowStarted(), executionStarted()));
  }

  private static OrchestratorActions.CompleteWorkflowAction completion(TaskOrchestratorResult result) {
    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      if (action.hasCompleteWorkflow()) {
        return action.getCompleteWorkflow();
      }
    }
    throw new AssertionError("the orchestration did not complete: " + result.getActions());
  }

  @Test
  public void shouldCollectEveryResultInOrderWhenAllTasksSucceed() {
    AtomicReference<List<String>> collected = new AtomicReference<>();

    TaskOrchestration orchestration = ctx -> {
      List<Task<String>> tasks = Arrays.asList(
          ctx.callActivity("First", null, null, String.class),
          ctx.callActivity("Second", null, null, String.class));
      collected.set(ctx.allOf(tasks).await());
      ctx.complete("done");
    };

    TaskOrchestratorResult result = run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "First"), taskScheduled(1, "Second")),
        Arrays.asList(taskCompleted(0, "\"one\""), taskCompleted(1, "\"two\"")));

    assertEquals(Arrays.asList("one", "two"), collected.get(),
        "allOf returns results in the order the tasks were listed, not completion order");
    assertEquals(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED,
        completion(result).getWorkflowStatus());
  }

  @Test
  public void shouldReportEveryFailureTogetherWhenSomeTasksFail() {
    AtomicReference<CompositeTaskFailedException> caught = new AtomicReference<>();

    TaskOrchestration orchestration = ctx -> {
      List<Task<String>> tasks = Arrays.asList(
          ctx.callActivity("First", null, null, String.class),
          ctx.callActivity("Second", null, null, String.class));
      try {
        ctx.allOf(tasks).await();
      } catch (CompositeTaskFailedException e) {
        caught.set(e);
      }
      ctx.complete("done");
    };

    run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "First"), taskScheduled(1, "Second")),
        Arrays.asList(taskFailed(0, "first blew up"), taskFailed(1, "second blew up")));

    assertNotNull(caught.get(), "allOf must surface the failures rather than the first result");
    assertEquals(2, caught.get().getExceptions().size());
    assertTrue(caught.get().getMessage().contains("2 out of 2 tasks failed"), caught.get().getMessage());
  }

  @Test
  public void shouldStillListTheFailuresWhenOnlySomeTasksFail() {
    AtomicReference<CompositeTaskFailedException> caught = new AtomicReference<>();

    TaskOrchestration orchestration = ctx -> {
      List<Task<String>> tasks = Arrays.asList(
          ctx.callActivity("First", null, null, String.class),
          ctx.callActivity("Second", null, null, String.class));
      try {
        ctx.allOf(tasks).await();
      } catch (CompositeTaskFailedException e) {
        caught.set(e);
      }
      ctx.complete("done");
    };

    run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "First"), taskScheduled(1, "Second")),
        Arrays.asList(taskCompleted(0, "\"one\""), taskFailed(1, "second blew up")));

    assertNotNull(caught.get());
    assertEquals(1, caught.get().getExceptions().size(), "only the failed task is listed");
    assertTrue(caught.get().getMessage().contains("1 out of 2 tasks failed"), caught.get().getMessage());
  }

  @Test
  public void shouldReturnACompletedTaskFromAnyOf() {
    AtomicReference<Task<?>> winner = new AtomicReference<>();
    List<Task<String>> scheduled = new ArrayList<>();

    TaskOrchestration orchestration = ctx -> {
      scheduled.clear();
      scheduled.add(ctx.callActivity("First", null, null, String.class));
      scheduled.add(ctx.callActivity("Second", null, null, String.class));
      winner.set(ctx.anyOf(new ArrayList<>(scheduled)).await());
      ctx.complete("done");
    };

    run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "First"), taskScheduled(1, "Second")),
        Arrays.asList(taskCompleted(1, "\"two\"")));

    assertNotNull(winner.get());
    assertTrue(winner.get().isDone(), "anyOf returns a task that has completed");
    assertSame(scheduled.get(1), winner.get());
  }

  @Test
  public void shouldRejectAMissingTaskList() {
    AtomicReference<Class<?>> allOfFailure = new AtomicReference<>();
    AtomicReference<Class<?>> anyOfFailure = new AtomicReference<>();

    runFromStart(ctx -> {
      allOfFailure.set(assertThrows(IllegalArgumentException.class, () -> ctx.allOf((List<Task<String>>) null)).getClass());
      anyOfFailure.set(assertThrows(IllegalArgumentException.class, () -> ctx.anyOf((List<Task<?>>) null)).getClass());
      ctx.complete(null);
    });

    assertEquals(IllegalArgumentException.class, allOfFailure.get());
    assertEquals(IllegalArgumentException.class, anyOfFailure.get());
  }

  @Test
  public void shouldEmitASendEventActionForAnotherInstance() {
    TaskOrchestratorResult result = runFromStart(ctx -> {
      ctx.sendEvent("other-instance", "Approval", "the payload");
      ctx.complete(null);
    });

    OrchestratorActions.SendEventAction sent = result.getActions().stream()
        .filter(OrchestratorActions.WorkflowAction::hasSendEvent)
        .map(OrchestratorActions.WorkflowAction::getSendEvent)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no send-event action was emitted"));

    assertEquals("other-instance", sent.getInstance().getInstanceId());
    assertEquals("Approval", sent.getName());
    assertEquals("\"the payload\"", sent.getData().getValue());
  }

  @Test
  public void shouldSendAnEventWithNoDataWhenThereIsNoPayload() {
    TaskOrchestratorResult result = runFromStart(ctx -> {
      ctx.sendEvent("other-instance", "Ping", null);
      ctx.complete(null);
    });

    OrchestratorActions.SendEventAction sent = result.getActions().stream()
        .filter(OrchestratorActions.WorkflowAction::hasSendEvent)
        .map(OrchestratorActions.WorkflowAction::getSendEvent)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no send-event action was emitted"));

    assertTrue(sent.getData().getValue().isEmpty(), "a null payload is sent as no data");
  }

  @Test
  public void shouldRefuseToSendAnEventWithoutATargetInstance() {
    AtomicReference<String> message = new AtomicReference<>();

    runFromStart(ctx -> {
      message.set(assertThrows(IllegalArgumentException.class,
          () -> ctx.sendEvent("", "Approval", null)).getMessage());
      ctx.complete(null);
    });

    assertTrue(message.get().contains("instanceId"), message.get());
  }

  @Test
  public void shouldCreateATimerAndWaitForItRatherThanCompleting() {
    TaskOrchestratorResult result = runFromStart(ctx -> {
      ctx.createTimer(Duration.ofMinutes(5)).await();
      ctx.complete(null);
    });

    assertTrue(result.getActions().stream().anyMatch(OrchestratorActions.WorkflowAction::hasCreateTimer),
        "a timer action must reach the sidecar");
    assertTrue(result.getActions().stream().noneMatch(OrchestratorActions.WorkflowAction::hasCompleteWorkflow),
        "the orchestration is parked on the timer, so this turn must not complete it");
  }

  @Test
  public void shouldTreatAPatchAsAppliedOnAFirstRunAndStayConsistentAfterwards() {
    AtomicReference<Boolean> first = new AtomicReference<>();
    AtomicReference<Boolean> second = new AtomicReference<>();

    runFromStart(ctx -> {
      first.set(ctx.isPatched("new-behaviour"));
      second.set(ctx.isPatched("new-behaviour"));
      ctx.complete(null);
    });

    assertTrue(first.get(), "a patch checked on a brand new instance is applied");
    assertEquals(first.get(), second.get(), "the same patch must answer the same way within a run");
  }

  @Test
  public void shouldExposeTheInstanceIdAndAStableCurrentInstant() {
    AtomicReference<String> seenId = new AtomicReference<>();
    AtomicReference<Instant> firstRead = new AtomicReference<>();
    AtomicReference<Instant> secondRead = new AtomicReference<>();

    runFromStart(ctx -> {
      seenId.set(ctx.getInstanceId());
      firstRead.set(ctx.getCurrentInstant());
      secondRead.set(ctx.getCurrentInstant());
      ctx.complete(null);
    });

    assertEquals(INSTANCE_ID, seenId.get());
    assertEquals(TEST_INSTANT, firstRead.get(), "the instant comes from the workflow-started event");
    assertEquals(firstRead.get(), secondRead.get(),
        "the current instant must not move within one replay, or workflows stop being deterministic");
  }

  @Test
  public void shouldRefuseToActAfterTheOrchestrationCompleted() {
    AtomicReference<Class<?>> afterComplete = new AtomicReference<>();

    runFromStart(ctx -> {
      ctx.complete("done");
      afterComplete.set(assertThrows(IllegalStateException.class,
          () -> ctx.sendEvent("other-instance", "Approval", null)).getClass());
    });

    assertEquals(IllegalStateException.class, afterComplete.get());
  }

  @Test
  public void shouldScheduleARetryTimerWhenAnActivityWithAPolicyFails() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(3)
        .setFirstRetryInterval(Duration.ofSeconds(5))
        .build();

    TaskOrchestration orchestration = ctx -> {
      ctx.callActivity("Flaky", null, new WorkflowTaskOptions(policy), String.class).await();
      ctx.complete("done");
    };

    // First turn: the activity is scheduled.
    TaskOrchestratorResult first = runFromStart(orchestration);
    assertTrue(first.getActions().stream().anyMatch(OrchestratorActions.WorkflowAction::hasScheduleTask),
        "the first attempt must be scheduled");

    // Second turn: the attempt failed, so a retry timer is scheduled rather than the workflow failing.
    TaskOrchestratorResult second = run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "Flaky")),
        Arrays.asList(taskFailed(0, "flaky blew up")));

    assertTrue(second.getActions().stream().anyMatch(OrchestratorActions.WorkflowAction::hasCreateTimer),
        "a failure under a retry policy waits on a timer, it does not fail the workflow");
    assertTrue(second.getActions().stream().noneMatch(OrchestratorActions.WorkflowAction::hasCompleteWorkflow),
        "the workflow must still be running while it retries");
  }

  @Test
  public void shouldFailTheWorkflowOnceTheRetriesAreExhausted() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(1)
        .setFirstRetryInterval(Duration.ofSeconds(5))
        .build();

    TaskOrchestration orchestration = ctx -> {
      ctx.callActivity("Flaky", null, new WorkflowTaskOptions(policy), String.class).await();
      ctx.complete("done");
    };

    TaskOrchestratorResult result = run(orchestration,
        Arrays.asList(workflowStarted(), executionStarted(), taskScheduled(0, "Flaky")),
        Arrays.asList(taskFailed(0, "flaky blew up")));

    assertEquals(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_FAILED,
        completion(result).getWorkflowStatus(),
        "with a single attempt allowed there is nothing left to retry");
  }
}
