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
 * limitations under the License.
 */

package io.dapr.durabletask;

import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PropagatedHistory surfacing on TaskActivityExecutor.
 */
class ActivityHistoryPropagationTest {

  private static final Logger logger = Logger.getLogger(ActivityHistoryPropagationTest.class.getName());

  @Test
  void execute_withPropagatedHistory_surfacesOnContext() throws Throwable {
    final Optional<PropagatedHistory>[] captured = new Optional[]{Optional.empty()};

    HashMap<String, TaskActivityFactory> factories = new HashMap<>();
    factories.put("MyActivity", new TaskActivityFactory() {
      @Override
      public String getName() {
        return "MyActivity";
      }

      @Override
      public TaskActivity create() {
        return ctx -> {
          captured[0] = ctx.getPropagatedHistory();
          return "done";
        };
      }
    });

    TaskActivityExecutor executor = new TaskActivityExecutor(factories, new JacksonDataConverter(), logger);

    HistoryEvents.HistoryEvent event = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("ValidateCard").build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistoryProto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("parent-app")
            .addRawEvents(event.toByteString())
            .setInstanceId("parent-inst-1")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    executor.execute("MyActivity", "\"input\"", "exec-1", 0, "traceparent", propagatedHistoryProto);

    assertTrue(captured[0].isPresent());
    PropagatedHistory history = captured[0].get();
    assertEquals(HistoryPropagationScope.OWN_HISTORY, history.getScope());
    assertEquals(1, history.getEvents().size());
    assertEquals("ValidateCard", history.getEvents().get(0).getTaskScheduled().getName());
    assertEquals(1, history.getWorkflows().size());
    assertEquals("parent-app", history.getWorkflows().get(0).getAppId());
  }

  @Test
  void execute_withoutPropagatedHistory_returnsEmptyOptional() throws Throwable {
    final Optional<PropagatedHistory>[] captured = new Optional[]{Optional.empty()};

    HashMap<String, TaskActivityFactory> factories = new HashMap<>();
    factories.put("MyActivity", new TaskActivityFactory() {
      @Override
      public String getName() {
        return "MyActivity";
      }

      @Override
      public TaskActivity create() {
        return ctx -> {
          captured[0] = ctx.getPropagatedHistory();
          return "done";
        };
      }
    });

    TaskActivityExecutor executor = new TaskActivityExecutor(factories, new JacksonDataConverter(), logger);

    executor.execute("MyActivity", "\"input\"", "exec-1", 0, "traceparent");

    assertFalse(captured[0].isPresent());
  }

  @Test
  void execute_withNullPropagatedHistory_returnsEmptyOptional() throws Throwable {
    final Optional<PropagatedHistory>[] captured = new Optional[]{Optional.empty()};

    HashMap<String, TaskActivityFactory> factories = new HashMap<>();
    factories.put("MyActivity", new TaskActivityFactory() {
      @Override
      public String getName() {
        return "MyActivity";
      }

      @Override
      public TaskActivity create() {
        return ctx -> {
          captured[0] = ctx.getPropagatedHistory();
          return "done";
        };
      }
    });

    TaskActivityExecutor executor = new TaskActivityExecutor(factories, new JacksonDataConverter(), logger);

    executor.execute("MyActivity", "\"input\"", "exec-1", 0, "traceparent", null);

    assertFalse(captured[0].isPresent());
  }
}
