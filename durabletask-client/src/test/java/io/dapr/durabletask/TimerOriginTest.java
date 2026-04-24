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

package io.dapr.durabletask;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactories;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for timer origin assignment and backwards-compatible optional external-event timers.
 */
class TimerOriginTest {

  private static final Logger logger = Logger.getLogger(TimerOriginTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);
  private static final Instant TEST_INSTANT = Instant.parse("2024-01-01T00:00:00Z");
  private static final String TEST_INSTANCE = "test-instance";

  // ==================================================================================
  // History-event builders
  // ==================================================================================

  private static Timestamp ts(Instant instant) {
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  private static HistoryEvents.HistoryEvent workflowStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
  }

  private static HistoryEvents.HistoryEvent executionStarted(String name) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(name)
            .setWorkflowInstance(
                Orchestration.WorkflowInstance.newBuilder().setInstanceId(TEST_INSTANCE).build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent eventRaised(String name, String payload) {
    HistoryEvents.EventRaisedEvent.Builder b = HistoryEvents.EventRaisedEvent.newBuilder().setName(name);
    if (payload != null) {
      b.setInput(StringValue.of(payload));
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setEventRaised(b.build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskScheduled(int eventId, String name, String taskExecutionId) {
    HistoryEvents.TaskScheduledEvent.Builder b = HistoryEvents.TaskScheduledEvent.newBuilder().setName(name);
    if (taskExecutionId != null) {
      b.setTaskExecutionId(taskExecutionId);
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(b.build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskCompleted(int taskScheduledId, String result) {
    HistoryEvents.TaskCompletedEvent.Builder b = HistoryEvents.TaskCompletedEvent.newBuilder()
        .setTaskScheduledId(taskScheduledId);
    if (result != null) {
      b.setResult(StringValue.of(result));
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskCompleted(b.build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskFailed(int taskScheduledId, String errorMessage) {
    Orchestration.TaskFailureDetails details = Orchestration.TaskFailureDetails.newBuilder()
        .setErrorType("TestError")
        .setErrorMessage(errorMessage == null ? "" : errorMessage)
        .build();
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskFailed(HistoryEvents.TaskFailedEvent.newBuilder()
            .setTaskScheduledId(taskScheduledId)
            .setFailureDetails(details)
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent timerCreatedExternalEvent(int eventId, String eventName, Instant fireAt) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.newBuilder()
            .setFireAt(ts(fireAt))
            .setExternalEvent(HistoryEvents.TimerOriginExternalEvent.newBuilder().setName(eventName).build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent timerCreatedCreateTimer(int eventId, Instant fireAt) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.newBuilder()
            .setFireAt(ts(fireAt))
            .setCreateTimer(HistoryEvents.TimerOriginCreateTimer.getDefaultInstance())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent timerFired(int timerId, Instant fireAt) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerFired(HistoryEvents.TimerFiredEvent.newBuilder()
            .setTimerId(timerId)
            .setFireAt(ts(fireAt))
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childWorkflowInstanceCreated(int eventId, String instanceId, String name) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setChildWorkflowInstanceCreated(HistoryEvents.ChildWorkflowInstanceCreatedEvent.newBuilder()
            .setInstanceId(instanceId)
            .setName(name)
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childWorkflowInstanceCompleted(int taskScheduledId, String result) {
    HistoryEvents.ChildWorkflowInstanceCompletedEvent.Builder b = HistoryEvents.ChildWorkflowInstanceCompletedEvent
        .newBuilder()
        .setTaskScheduledId(taskScheduledId);
    if (result != null) {
      b.setResult(StringValue.of(result));
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setChildWorkflowInstanceCompleted(b.build())
        .build();
  }

  // ==================================================================================
  // Executor helpers
  // ==================================================================================

  private TaskOrchestrationExecutor createExecutor(String orchestratorName, TaskOrchestration orchestration) {
    TaskOrchestrationFactories factories = new TaskOrchestrationFactories();
    factories.addOrchestration(new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return orchestratorName;
      }

      @Override
      public TaskOrchestration create() {
        return orchestration;
      }

      @Override
      public String getVersionName() {
        return null;
      }

      @Override
      public Boolean isLatestVersion() {
        return false;
      }
    });
    return new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, logger, null);
  }

  private static OrchestratorActions.WorkflowAction findSingleTimerAction(TaskOrchestratorResult result) {
    List<OrchestratorActions.WorkflowAction> timers = new ArrayList<>();
    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      if (action.hasCreateTimer()) {
        timers.add(action);
      }
    }
    assertEquals(1, timers.size(), "expected exactly one CreateTimer action, got " + timers.size());
    return timers.get(0);
  }

  private static OrchestratorActions.WorkflowAction findFirstWithType(
      TaskOrchestratorResult result,
      java.util.function.Predicate<OrchestratorActions.WorkflowAction> predicate) {
    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      if (predicate.test(action)) {
        return action;
      }
    }
    fail("no matching action found in result");
    return null;
  }

  // ==================================================================================
  // Test 1 - CreateTimer(delay) sets TimerOriginCreateTimer
  // ==================================================================================

  @Test
  void test1_createTimerSetsCreateTimerOrigin() {
    TaskOrchestration orchestration = ctx -> ctx.createTimer(Duration.ofSeconds(5)).await();

    TaskOrchestrationExecutor executor = createExecutor("Orch1", orchestration);

    TaskOrchestratorResult result = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch1")));

    OrchestratorActions.WorkflowAction timer = findSingleTimerAction(result);
    assertTrue(timer.getCreateTimer().hasCreateTimer(),
        "CreateTimer action must have TimerOriginCreateTimer");
    assertFalse(timer.getCreateTimer().hasExternalEvent());
    assertFalse(timer.getCreateTimer().hasActivityRetry());
    assertFalse(timer.getCreateTimer().hasChildWorkflowRetry());
  }

  // ==================================================================================
  // Test 2 - finite-timeout WaitForExternalEvent sets TimerOriginExternalEvent
  // ==================================================================================

  @Test
  void test2_finiteTimeoutWaitForExternalEventSetsExternalEventOrigin() {
    Duration timeout = Duration.ofSeconds(10);
    TaskOrchestration orchestration = ctx -> ctx.waitForExternalEvent("myEvent", timeout, String.class).await();

    TaskOrchestrationExecutor executor = createExecutor("Orch2", orchestration);

    TaskOrchestratorResult result = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch2")));

    OrchestratorActions.WorkflowAction timer = findSingleTimerAction(result);
    OrchestratorActions.CreateTimerAction ct = timer.getCreateTimer();
    assertTrue(ct.hasExternalEvent(), "origin must be ExternalEvent");
    assertEquals("myEvent", ct.getExternalEvent().getName());

    Instant expectedFireAt = TEST_INSTANT.plus(timeout);
    assertEquals(expectedFireAt.getEpochSecond(), ct.getFireAt().getSeconds());
    assertEquals(expectedFireAt.getNano(), ct.getFireAt().getNanos());
  }

  // ==================================================================================
  // Test 3 - activity retry timer sets TimerOriginActivityRetry
  // ==================================================================================

  @Test
  void test3_activityRetryTimerSetsActivityRetryOrigin() {
    RetryPolicy policy = new RetryPolicy(2, Duration.ofSeconds(1));
    TaskOptions options = TaskOptions.withRetryPolicy(policy);

    TaskOrchestration orchestration = ctx -> {
      try {
        ctx.callActivity("myActivity", null, options, String.class).await();
      } catch (TaskFailedException e) {
        // expected during replay with a failed attempt
      }
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch3", orchestration);

    // Phase 1: run with no history to discover the taskExecutionId the orchestration emits.
    TaskOrchestratorResult phase1 = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch3")));
    OrchestratorActions.WorkflowAction schedule = findFirstWithType(phase1,
        OrchestratorActions.WorkflowAction::hasScheduleTask);
    String taskExecutionId = schedule.getScheduleTask().getTaskExecutionId();
    assertFalse(taskExecutionId.isEmpty(), "ScheduleTaskAction must carry a taskExecutionId");

    // Phase 2: simulate the activity being scheduled (with that taskExecutionId) and failing.
    // Expect the retry delay CreateTimer to have origin=ActivityRetry{taskExecutionId}.
    TaskOrchestratorResult phase2 = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch3"),
            taskScheduled(0, "myActivity", taskExecutionId),
            taskFailed(0, "boom")),
        new ArrayList<>());
    OrchestratorActions.WorkflowAction retryTimer = findSingleTimerAction(phase2);
    OrchestratorActions.CreateTimerAction ct = retryTimer.getCreateTimer();
    assertTrue(ct.hasActivityRetry(), "retry timer must have ActivityRetry origin");
    assertEquals(taskExecutionId, ct.getActivityRetry().getTaskExecutionId(),
        "retry timer taskExecutionId must equal the scheduled activity's taskExecutionId");
  }

  // ==================================================================================
  // Test 4 - activity retry taskExecutionId is stable across attempts
  // ==================================================================================

  @Test
  void test4_activityRetryTaskExecutionIdStable() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    TaskOptions options = TaskOptions.withRetryPolicy(policy);

    TaskOrchestration orchestration = ctx -> {
      try {
        ctx.callActivity("myActivity", null, options, String.class).await();
      } catch (TaskFailedException e) {
        // swallow
      }
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch4", orchestration);

    // Discover the stable taskExecutionId first.
    TaskOrchestratorResult phase1 = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch4")));
    OrchestratorActions.WorkflowAction schedule = findFirstWithType(phase1,
        OrchestratorActions.WorkflowAction::hasScheduleTask);
    String taskExecutionId = schedule.getScheduleTask().getTaskExecutionId();

    // History simulates two scheduled-then-failed attempts with a retry timer in between.
    // A second WorkflowStarted advances currentInstant to the timer's fireAt before TimerFired
    // is processed — otherwise the TimerTask chain would emit an extra sub-timer for the
    // "remaining" delay and derail subsequent sequence-number matching.
    // The next emitted action should be the SECOND retry-delay timer, which must carry
    // the same taskExecutionId as the original ScheduleTaskAction.
    Instant retry1FireAt = TEST_INSTANT.plusSeconds(1);
    TaskOrchestratorResult phase2 = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch4"),
            taskScheduled(0, "myActivity", taskExecutionId),
            taskFailed(0, "fail-1"),
            timerCreatedActivityRetry(1, retry1FireAt, taskExecutionId),
            workflowStartedAt(retry1FireAt),
            timerFired(1, retry1FireAt),
            taskScheduled(2, "myActivity", taskExecutionId),
            taskFailed(2, "fail-2")),
        new ArrayList<>());

    OrchestratorActions.WorkflowAction retryTimer = findSingleTimerAction(phase2);
    OrchestratorActions.CreateTimerAction ct = retryTimer.getCreateTimer();
    assertTrue(ct.hasActivityRetry());
    assertEquals(taskExecutionId, ct.getActivityRetry().getTaskExecutionId());
  }

  private static HistoryEvents.HistoryEvent workflowStartedAt(Instant instant) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(instant))
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
  }

  private static HistoryEvents.HistoryEvent timerCreatedActivityRetry(int eventId, Instant fireAt,
                                                                      String taskExecutionId) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.newBuilder()
            .setFireAt(ts(fireAt))
            .setActivityRetry(HistoryEvents.TimerOriginActivityRetry.newBuilder()
                .setTaskExecutionId(taskExecutionId).build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent timerCreatedChildWorkflowRetry(int eventId, Instant fireAt,
                                                                           String instanceId) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.newBuilder()
            .setFireAt(ts(fireAt))
            .setChildWorkflowRetry(HistoryEvents.TimerOriginChildWorkflowRetry.newBuilder()
                .setInstanceId(instanceId).build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childWorkflowInstanceFailed(int taskScheduledId, String errorMessage) {
    Orchestration.TaskFailureDetails details = Orchestration.TaskFailureDetails.newBuilder()
        .setErrorType("TestError")
        .setErrorMessage(errorMessage == null ? "" : errorMessage)
        .build();
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setChildWorkflowInstanceFailed(HistoryEvents.ChildWorkflowInstanceFailedEvent.newBuilder()
            .setTaskScheduledId(taskScheduledId)
            .setFailureDetails(details)
            .build())
        .build();
  }

  // ==================================================================================
  // Test 5 - child workflow retry timer sets TimerOriginChildWorkflowRetry
  // ==================================================================================

  @Test
  void test5_childWorkflowRetryTimerSetsChildWorkflowRetryOrigin() {
    RetryPolicy policy = new RetryPolicy(2, Duration.ofSeconds(1));
    TaskOptions options = TaskOptions.withRetryPolicy(policy);
    String childInstanceId = "child-1";

    TaskOrchestration orchestration = ctx -> {
      try {
        ctx.callSubOrchestrator("Child", null, childInstanceId, options, String.class).await();
      } catch (TaskFailedException e) {
        // swallow
      }
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch5", orchestration);

    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch5"),
            childWorkflowInstanceCreated(0, childInstanceId, "Child"),
            childWorkflowInstanceFailed(0, "boom")),
        new ArrayList<>());

    OrchestratorActions.WorkflowAction retryTimer = findSingleTimerAction(result);
    OrchestratorActions.CreateTimerAction ct = retryTimer.getCreateTimer();
    assertTrue(ct.hasChildWorkflowRetry(), "retry timer must have ChildWorkflowRetry origin");
    assertEquals(childInstanceId, ct.getChildWorkflowRetry().getInstanceId());
  }

  // ==================================================================================
  // Test 6 - child workflow retry instanceId always points to first child
  // ==================================================================================

  @Test
  void test6_childWorkflowRetryInstanceIdStaysOnFirstChild() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    TaskOptions options = TaskOptions.withRetryPolicy(policy);
    String firstChildInstanceId = "child-1";

    TaskOrchestration orchestration = ctx -> {
      try {
        ctx.callSubOrchestrator("Child", null, firstChildInstanceId, options, String.class).await();
      } catch (TaskFailedException e) {
        // swallow
      }
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch6", orchestration);

    // Two failed attempts. Second retry-delay timer must still carry the first child's instance id
    // even though the second attempt was scheduled under a (hypothetically) different instance.
    // A second WorkflowStarted advances currentInstant so the TimerTask chain doesn't emit an
    // extra sub-timer after TimerFired (see Test 4 for the same pattern).
    Instant retry1FireAt = TEST_INSTANT.plusSeconds(1);
    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch6"),
            childWorkflowInstanceCreated(0, firstChildInstanceId, "Child"),
            childWorkflowInstanceFailed(0, "fail-1"),
            timerCreatedChildWorkflowRetry(1, retry1FireAt, firstChildInstanceId),
            workflowStartedAt(retry1FireAt),
            timerFired(1, retry1FireAt),
            childWorkflowInstanceCreated(2, firstChildInstanceId, "Child"),
            childWorkflowInstanceFailed(2, "fail-2")),
        new ArrayList<>());

    OrchestratorActions.WorkflowAction retryTimer = findSingleTimerAction(result);
    OrchestratorActions.CreateTimerAction ct = retryTimer.getCreateTimer();
    assertTrue(ct.hasChildWorkflowRetry());
    assertEquals(firstChildInstanceId, ct.getChildWorkflowRetry().getInstanceId(),
        "second retry timer must still carry the first child's instance id");
  }

  // ==================================================================================
  // Test 7 - indefinite WaitForExternalEvent emits sentinel optional timer
  // ==================================================================================

  @Test
  void test7_indefiniteWaitForExternalEventEmitsSentinelOptionalTimer() {
    TaskOrchestration orchestration = ctx ->
        ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(-1), String.class).await();

    TaskOrchestrationExecutor executor = createExecutor("Orch7", orchestration);

    TaskOrchestratorResult result = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch7")));

    OrchestratorActions.WorkflowAction timer = findSingleTimerAction(result);
    OrchestratorActions.CreateTimerAction ct = timer.getCreateTimer();
    assertTrue(ct.hasExternalEvent(), "origin must be ExternalEvent");
    assertEquals("myEvent", ct.getExternalEvent().getName());

    Instant sentinel = TaskOrchestrationExecutor.EXTERNAL_EVENT_INDEFINITE_FIRE_AT;
    assertEquals(sentinel.getEpochSecond(), ct.getFireAt().getSeconds(),
        "fireAt seconds must equal the sentinel");
    assertEquals(sentinel.getNano(), ct.getFireAt().getNanos(),
        "fireAt nanos must equal the sentinel (to nanosecond precision)");
  }

  // ==================================================================================
  // Test 8 - zero-timeout WaitForExternalEvent emits no timer
  // ==================================================================================

  @Test
  void test8_zeroTimeoutWaitForExternalEventEmitsNoTimer() {
    TaskOrchestration orchestration = ctx -> {
      try {
        ctx.waitForExternalEvent("myEvent", Duration.ZERO, String.class).await();
      } catch (TaskCanceledException e) {
        // expected
      }
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch8", orchestration);

    TaskOrchestratorResult result = executor.execute(
        new ArrayList<>(),
        List.of(workflowStarted(), executionStarted("Orch8")));

    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      assertFalse(action.hasCreateTimer(), "no CreateTimer should be emitted for zero-timeout wait");
    }
  }

  // ==================================================================================
  // Test 9 - post-patch replay matches the optional timer normally
  // ==================================================================================

  @Test
  void test9_postPatchReplayMatchesOptionalTimerNormally() {
    TaskOrchestration orchestration = ctx ->
        ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(-1), String.class).await();

    TaskOrchestrationExecutor executor = createExecutor("Orch9", orchestration);

    // Post-patch history: optional timer was emitted by the new code. ExternalEventTask
    // consumes sequence id 0 in the Java SDK, so the optional timer lands at id 1.
    Instant sentinel = TaskOrchestrationExecutor.EXTERNAL_EVENT_INDEFINITE_FIRE_AT;
    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch9"),
            timerCreatedExternalEvent(1, "myEvent", sentinel)),
        List.of(eventRaised("myEvent", "\"payload\"")));

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size(), "expected a single CompleteWorkflow action");
    assertTrue(actions.get(0).hasCompleteWorkflow());
  }

  // ==================================================================================
  // Test 10 - pre-patch replay, indefinite wait followed by CallActivity
  // ==================================================================================

  @Test
  void test10_prePatchReplayIndefiniteWaitThenCallActivity() {
    TaskOrchestration orchestration = ctx -> {
      ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(-1), String.class).await();
      ctx.callActivity("A", null, null, String.class).await();
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch10", orchestration);

    // Pre-patch Java history: ExternalEventTask reserves sequence id 0; the activity lands at id 1.
    // On post-patch code the optional timer sits at id 1, so the incoming TaskScheduled(eventId=1)
    // forces the drop-and-shift.
    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch10"),
            eventRaised("myEvent", null),
            taskScheduled(1, "A", "exec-A")),
        List.of(taskCompleted(1, "\"result\"")));

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size(), "expected a single CompleteWorkflow action");
    assertTrue(actions.get(0).hasCompleteWorkflow());
    for (OrchestratorActions.WorkflowAction action : actions) {
      assertFalse(action.hasCreateTimer(),
          "the optional timer must be dropped, not flushed to history");
    }
  }

  // ==================================================================================
  // Test 11 - pre-patch replay, indefinite wait followed by CallChildWorkflow
  // ==================================================================================

  @Test
  void test11_prePatchReplayIndefiniteWaitThenCallChildWorkflow() {
    TaskOrchestration orchestration = ctx -> {
      ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(-1), String.class).await();
      ctx.callSubOrchestrator("Child", null, "child-1", null, String.class).await();
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch11", orchestration);

    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch11"),
            eventRaised("myEvent", null),
            childWorkflowInstanceCreated(1, "child-1", "Child")),
        List.of(childWorkflowInstanceCompleted(1, "\"result\"")));

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());
    assertTrue(actions.get(0).hasCompleteWorkflow());
    for (OrchestratorActions.WorkflowAction action : actions) {
      assertFalse(action.hasCreateTimer());
    }
  }

  // ==================================================================================
  // Test 12 - pre-patch replay, indefinite wait followed by user CreateTimer
  // ==================================================================================

  @Test
  void test12_prePatchReplayIndefiniteWaitThenCreateTimer() {
    TaskOrchestration orchestration = ctx -> {
      ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(-1), String.class).await();
      ctx.createTimer(Duration.ofSeconds(5)).await();
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch12", orchestration);

    // Pre-patch history: no optional timer at id 1, instead the user CreateTimer is at id 1
    // with a non-sentinel fireAt and a CreateTimer origin. The asymmetric branch in
    // handleTimerCreated must drop-and-shift before the normal match.
    Instant userTimerFireAt = TEST_INSTANT.plusSeconds(5);
    // Advance currentInstant on the second WorkflowStarted so the short-timer chain completes.
    HistoryEvents.HistoryEvent workflowStartedAfterTimer = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(userTimerFireAt))
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch12"),
            eventRaised("myEvent", null),
            timerCreatedCreateTimer(1, userTimerFireAt)),
        List.of(workflowStartedAfterTimer, timerFired(1, userTimerFireAt)));

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size(), "expected a single CompleteWorkflow action");
    assertTrue(actions.get(0).hasCompleteWorkflow());
  }

  // ==================================================================================
  // Test 13 - pre-patch replay, two indefinite waits in sequence
  // ==================================================================================

  @Test
  void test13_prePatchReplayTwoIndefiniteWaitsInSequence() {
    TaskOrchestration orchestration = ctx -> {
      ctx.waitForExternalEvent("A", Duration.ofSeconds(-1), String.class).await();
      ctx.callActivity("ActA", null, null, String.class).await();
      ctx.waitForExternalEvent("B", Duration.ofSeconds(-1), String.class).await();
      ctx.callActivity("ActB", null, null, String.class).await();
    };

    TaskOrchestrationExecutor executor = createExecutor("Orch13", orchestration);

    // Pre-patch Java history: waitA reserves id 0 (no action), ActA at id 1, ActA completes,
    // waitB reserves id 2 (no action), ActB at id 3. Drop-and-shift must compose correctly
    // across the two indefinite waits.
    TaskOrchestratorResult result = executor.execute(
        List.of(
            workflowStarted(),
            executionStarted("Orch13"),
            eventRaised("A", null),
            taskScheduled(1, "ActA", "exec-A"),
            taskCompleted(1, "\"A-result\""),
            eventRaised("B", null),
            taskScheduled(3, "ActB", "exec-B")),
        List.of(taskCompleted(3, "\"B-result\"")));

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size(), "expected a single CompleteWorkflow action");
    assertTrue(actions.get(0).hasCompleteWorkflow());
    for (OrchestratorActions.WorkflowAction action : actions) {
      assertFalse(action.hasCreateTimer(),
          "neither optional timer should leak into the result");
    }
  }
}
