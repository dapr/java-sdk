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
 * limitations under the License.
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
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces an agent-loop stall: the final external event arrives in the
 * same work item as the activity completion that resumes the orchestrator,
 * and must both complete the re-armed wait and not leave its obsolete timer
 * blocking implicit completion. History reconstructed from a failing
 * production instance.
 */
class AgentLoopEventDeliveryTest {

  private static final Logger logger = Logger.getLogger(AgentLoopEventDeliveryTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);
  private static final Instant TEST_INSTANT = Instant.parse("2026-06-12T11:53:03Z");
  private static final String TEST_INSTANCE = "agent-loop-instance";
  private static final String EVENT_NAME = "agent-event";

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

  private static HistoryEvents.HistoryEvent eventRaised(String jsonPayload) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setEventRaised(HistoryEvents.EventRaisedEvent.newBuilder()
            .setName(EVENT_NAME)
            .setInput(StringValue.of(jsonPayload))
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskScheduled(int eventId, String name) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName(name)
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskCompleted(int taskScheduledId, String jsonResult) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskCompleted(HistoryEvents.TaskCompletedEvent.newBuilder()
            .setTaskScheduledId(taskScheduledId)
            .setResult(StringValue.of(jsonResult))
            .build())
        .build();
  }

  // Synthetic "optional" timer emitted by an indefinite waitForExternalEvent:
  // origin ExternalEvent, fireAt = the indefinite-wait sentinel.
  private static HistoryEvents.HistoryEvent sentinelTimerCreated(int eventId) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(ts(TEST_INSTANT))
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.newBuilder()
            .setFireAt(ts(TaskOrchestrationExecutor.EXTERNAL_EVENT_INDEFINITE_FIRE_AT))
            .setExternalEvent(
                HistoryEvents.TimerOriginExternalEvent.newBuilder().setName(EVENT_NAME).build())
            .build())
        .build();
  }

  // ==================================================================================
  // The agent-loop orchestrator
  // ==================================================================================

  private static TaskOrchestration agentLoop(boolean explicitComplete) {
    return ctx -> {
      while (true) {
        String payload = ctx.waitForExternalEvent(EVENT_NAME, null, String.class).await();
        if ("done".equals(payload)) {
          if (explicitComplete) {
            ctx.complete("agent-done");
          }
          return;
        }
        if ("llm".equals(payload)) {
          ctx.callActivity("llm-call", null, null, String.class).await();
        } else {
          ctx.callActivity("tool-call", null, null, String.class).await();
        }
      }
    };
  }

  private TaskOrchestrationExecutor createExecutor() {
    return createExecutor(true);
  }

  private TaskOrchestrationExecutor createExecutor(boolean explicitComplete) {
    TaskOrchestrationFactories factories = new TaskOrchestrationFactories();
    factories.addOrchestration(new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return "AgentLoop";
      }

      @Override
      public TaskOrchestration create() {
        return agentLoop(explicitComplete);
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

  // Past events reconstructed from the stuck instance (24 events, six turns).
  private static List<HistoryEvents.HistoryEvent> agentLoopHistory() {
    return List.of(
        // Turn 1: started, iter:0 wait armed (event task id=0, sentinel id=1)
        workflowStarted(), executionStarted("AgentLoop"), sentinelTimerCreated(1),
        // Turn 2: event "llm" completes iter:0 wait; llm-call scheduled (id=2)
        workflowStarted(), eventRaised("\"llm\""), taskScheduled(2, "llm-call"),
        // Turn 3: llm completes; iter:1 wait (ids 3,4); event "tool"; tool-call (id=5)
        workflowStarted(), taskCompleted(2, "\"ok\""), eventRaised("\"tool\""),
        sentinelTimerCreated(4), taskScheduled(5, "tool-call"),
        // Turn 4: event buffered before completion; iter:2 wait buffer-hits (id=6,
        // no sentinel); tool-call (id=7)
        workflowStarted(), eventRaised("\"tool\""), taskCompleted(5, "\"ok\""),
        taskScheduled(7, "tool-call"),
        // Turn 5: same shape; iter:3 wait buffer-hits (id=8); tool-call (id=9)
        workflowStarted(), eventRaised("\"tool\""), taskCompleted(7, "\"ok\""),
        taskScheduled(9, "tool-call"),
        // Turn 6: tool completes; iter:4 wait (ids 10,11); event "llm"; llm-call (id=12)
        workflowStarted(), taskCompleted(9, "\"ok\""), eventRaised("\"llm\""),
        sentinelTimerCreated(11), taskScheduled(12, "llm-call"));
  }

  private static void assertCompleted(TaskOrchestratorResult result) {
    StringBuilder actions = new StringBuilder();
    boolean completed = false;
    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      actions.append(action.getWorkflowActionTypeCase()).append(' ');
      if (action.hasCompleteWorkflow()
          && action.getCompleteWorkflow().getWorkflowStatus()
              == Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED) {
        completed = true;
      }
    }
    assertTrue(completed,
        "the 'done' event must complete the workflow, but actions were: [" + actions + "]");
  }

  // Completion precedes the event in the batch; the event must complete the
  // waiter registered in the same execution.
  @Test
  void doneEventDeliveredWhenBatchedAfterActivityCompletion() {
    TaskOrchestratorResult result = createExecutor().execute(
        agentLoopHistory(),
        List.of(workflowStarted(), taskCompleted(12, "\"guide\""), eventRaised("\"done\"")));
    assertCompleted(result);
  }

  // Mirrored ordering: the event precedes the completion, gets buffered, and
  // the wait registered afterwards must consume it from the buffer.
  @Test
  void doneEventDeliveredWhenBatchedBeforeActivityCompletion() {
    TaskOrchestratorResult result = createExecutor().execute(
        agentLoopHistory(),
        List.of(workflowStarted(), eventRaised("\"done\""), taskCompleted(12, "\"guide\"")));
    assertCompleted(result);
  }

  // The production stall: no explicit ctx.complete — the resolved wait's
  // pending timer must not block implicit completion (before the fix this
  // yielded [CreateTimer] and the workflow stayed RUNNING).
  @Test
  void implicitCompletionNotBlockedByResolvedWaitTimer() {
    TaskOrchestratorResult result = createExecutor(false).execute(
        agentLoopHistory(),
        List.of(workflowStarted(), taskCompleted(12, "\"guide\""), eventRaised("\"done\"")));
    assertCompleted(result);
  }

  // Without the event in the batch, re-arming the wait and yielding a single
  // sentinel CreateTimer is the correct response.
  @Test
  void missingEventYieldsSentinelTimerOnly() {
    TaskOrchestratorResult result = createExecutor().execute(
        agentLoopHistory(),
        List.of(workflowStarted(), taskCompleted(12, "\"guide\"")));

    int timers = 0;
    for (OrchestratorActions.WorkflowAction action : result.getActions()) {
      assertTrue(action.hasCreateTimer(),
          "expected only CreateTimer actions, got " + action.getWorkflowActionTypeCase());
      timers++;
    }
    assertTrue(timers == 1, "expected exactly one sentinel CreateTimer, got " + timers);
  }
}
