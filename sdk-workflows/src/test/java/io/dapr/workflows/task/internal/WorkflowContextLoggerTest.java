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
import io.dapr.workflows.task.TaskOrchestration;
import io.dapr.workflows.task.orchestration.TaskOrchestrationFactories;
import io.dapr.workflows.task.orchestration.TaskOrchestrationFactory;
import io.dapr.workflows.task.serialization.JacksonDataConverter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

/**
 * Pins the replay-silencing contract of {@code WorkflowContext.getLogger()}.
 *
 * <p>This behaviour used to live in the DefaultWorkflowContext adapter and was covered by
 * DefaultWorkflowContextTest. Folding the durable task client in moved it verbatim onto the
 * executor's context implementation, and deleting the adapter took its test with it. The
 * implementation is a private inner class, so it is exercised here by driving the executor:
 * events supplied as PAST history replay, events supplied as NEW do not.
 *
 * <p>The assertion is on the branch decision rather than on the returned logger's behaviour. This
 * module has no SLF4J provider on its test classpath, so {@code LoggerFactory.getLogger} returns
 * the same NOP singleton the replay branch returns — comparing instances would pass even if the
 * replay branch were deleted. Statically mocking the factory pins the real contract: it must not
 * be consulted at all while replaying.
 */
public class WorkflowContextLoggerTest {

  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);
  private static final String ORCHESTRATOR = "LoggerProbeOrchestrator";

  // The executor's own logger is java.util.logging; the context's getLogger() returns slf4j.
  private final java.util.logging.Logger executorLogger =
      java.util.logging.Logger.getLogger(WorkflowContextLoggerTest.class.getName());

  @Test
  public void loggerIsSilencedWhileReplayingAndLiveOtherwise() {
    Logger realLogger = mock(Logger.class);

    try (MockedStatic<LoggerFactory> factory = mockStatic(LoggerFactory.class)) {
      factory.when(() -> LoggerFactory.getLogger(anyString())).thenReturn(realLogger);

      AtomicReference<Logger> captured = new AtomicReference<>();
      AtomicReference<Boolean> replaying = new AtomicReference<>();
      TaskOrchestration orchestration = ctx -> {
        captured.set(ctx.getLogger());
        replaying.set(ctx.isReplaying());
      };

      // Replaying: the body runs while the executor walks PAST history.
      execute(orchestration, new ArrayList<>(List.of(orchestratorStarted(), executionStarted())),
          List.of(orchestratorCompleted()));

      assertTrue(Boolean.TRUE.equals(replaying.get()), "sanity: this run must be a replay");
      assertSame(NOPLogger.NOP_LOGGER, captured.get(),
          "a replaying context must return the no-op logger so workflow logs are emitted once");
      factory.verify(() -> LoggerFactory.getLogger(anyString()), never());

      // Not replaying: with no past history the executor is done replaying before the first event.
      execute(orchestration, new ArrayList<>(),
          List.of(orchestratorStarted(), executionStarted(), orchestratorCompleted()));

      assertTrue(Boolean.FALSE.equals(replaying.get()), "sanity: this run must not be a replay");
      assertSame(realLogger, captured.get(), "a live context must return a real logger");
      factory.verify(() -> LoggerFactory.getLogger(ORCHESTRATOR));
    }
  }

  private void execute(TaskOrchestration orchestration,
                       List<HistoryEvents.HistoryEvent> pastEvents,
                       List<HistoryEvents.HistoryEvent> newEvents) {
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
        return null;
      }

      @Override
      public Boolean isLatestVersion() {
        return false;
      }
    });

    new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, executorLogger, null)
        .execute(pastEvents, newEvents);
  }

  private static HistoryEvents.HistoryEvent orchestratorStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
  }

  private static HistoryEvents.HistoryEvent executionStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(ORCHESTRATOR)
            .setWorkflowInstance(
                Orchestration.WorkflowInstance.newBuilder().setInstanceId("instance-1").build())
            .setInput(StringValue.of("\"hello\""))
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent orchestratorCompleted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setWorkflowCompleted(HistoryEvents.WorkflowCompletedEvent.newBuilder().build())
        .build();
  }
}
