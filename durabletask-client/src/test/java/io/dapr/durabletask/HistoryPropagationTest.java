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

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactories;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for workflow history propagation in TaskOrchestrationExecutor.
 */
class HistoryPropagationTest {

  private static final Logger logger = Logger.getLogger(HistoryPropagationTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);

  private static HistoryEvents.HistoryEvent orchestratorStarted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setWorkflowStarted(HistoryEvents.WorkflowStartedEvent.newBuilder().build())
        .build();
  }

  private static HistoryEvents.HistoryEvent executionStarted(String name, String instanceId, String input) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(name)
            .setWorkflowInstance(
                Orchestration.WorkflowInstance.newBuilder().setInstanceId(instanceId).build())
            .setInput(StringValue.of(input))
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

  private TaskOrchestrationExecutor createExecutor(String orchestratorName, TaskOrchestration orchestration) {
    return createExecutor(orchestratorName, orchestration, null);
  }

  private TaskOrchestrationExecutor createExecutor(String orchestratorName, TaskOrchestration orchestration,
      String appId) {
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
    return new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, logger, appId);
  }

  // ==================================================================================
  // Tests for HistoryPropagationScope on ScheduleTaskAction (activity calls)
  // ==================================================================================

  @Test
  void callActivity_withLineageScope_setsHistoryPropagationScopeOnAction() {
    final String orchestratorName = "ParentOrchestrator";
    final String activityName = "MyActivity";

    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.LINEAGE)
          .build();
      ctx.callActivity(activityName, "input", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorActions.WorkflowAction action = actions.get(0);
    assertTrue(action.hasScheduleTask());

    OrchestratorActions.ScheduleTaskAction scheduleTask = action.getScheduleTask();
    assertEquals(activityName, scheduleTask.getName());
    assertTrue(scheduleTask.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        scheduleTask.getHistoryPropagationScope());
  }

  @Test
  void callActivity_withOwnHistoryScope_setsHistoryPropagationScopeOnAction() {
    final String orchestratorName = "ParentOrchestrator";
    final String activityName = "MyActivity";

    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.OWN_HISTORY)
          .build();
      ctx.callActivity(activityName, "input", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    OrchestratorActions.ScheduleTaskAction scheduleTask = actions.get(0).getScheduleTask();
    assertTrue(scheduleTask.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY,
        scheduleTask.getHistoryPropagationScope());
  }

  @Test
  void callActivity_withoutScope_doesNotSetHistoryPropagationScope() {
    final String orchestratorName = "ParentOrchestrator";
    final String activityName = "MyActivity";

    TaskOrchestration orchestration = ctx -> {
      ctx.callActivity(activityName, "input", null, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    OrchestratorActions.ScheduleTaskAction scheduleTask = actions.get(0).getScheduleTask();
    assertFalse(scheduleTask.hasHistoryPropagationScope());
  }

  // ==================================================================================
  // Tests for HistoryPropagationScope on CreateChildWorkflowAction (sub-orchestrations)
  // ==================================================================================

  @Test
  void callSubOrchestrator_withLineageScope_setsHistoryPropagationScopeOnAction() {
    final String orchestratorName = "ParentOrchestrator";
    final String childName = "ChildOrchestrator";

    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.LINEAGE)
          .build();
      ctx.callSubOrchestrator(childName, "input", "child-1", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorActions.WorkflowAction action = actions.get(0);
    assertTrue(action.hasCreateChildWorkflow());

    OrchestratorActions.CreateChildWorkflowAction createChild = action.getCreateChildWorkflow();
    assertEquals(childName, createChild.getName());
    assertTrue(createChild.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        createChild.getHistoryPropagationScope());
  }

  @Test
  void callSubOrchestrator_withOwnHistoryScope_setsHistoryPropagationScopeOnAction() {
    final String orchestratorName = "ParentOrchestrator";
    final String childName = "ChildOrchestrator";

    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.OWN_HISTORY)
          .build();
      ctx.callSubOrchestrator(childName, "input", "child-1", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    OrchestratorActions.CreateChildWorkflowAction createChild = actions.get(0).getCreateChildWorkflow();
    assertTrue(createChild.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY,
        createChild.getHistoryPropagationScope());
  }

  @Test
  void callSubOrchestrator_withoutScope_doesNotSetHistoryPropagationScope() {
    final String orchestratorName = "ParentOrchestrator";
    final String childName = "ChildOrchestrator";

    TaskOrchestration orchestration = ctx -> {
      ctx.callSubOrchestrator(childName, "input", "child-1", null, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-1", "\"hello\""),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(result.getActions());
    OrchestratorActions.CreateChildWorkflowAction createChild = actions.get(0).getCreateChildWorkflow();
    assertFalse(createChild.hasHistoryPropagationScope());
  }

  // ==================================================================================
  // Tests for PropagatedHistory received by orchestrator
  // ==================================================================================

  @Test
  void execute_withPropagatedHistory_surfacesHistoryOnContext() {
    final String orchestratorName = "ChildOrchestrator";
    final Optional<PropagatedHistory>[] captured = new Optional[]{Optional.empty()};

    TaskOrchestration orchestration = ctx -> {
      captured[0] = ctx.getPropagatedHistory();
      ctx.complete(null);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    // Build a PropagatedHistory proto
    HistoryEvents.HistoryEvent taskScheduled = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("ValidateCard")
            .build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistoryProto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("payment-app")
            .addRawEvents(taskScheduled.toByteString())
            .setInstanceId("parent-instance-1")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "child-1", "\"input\""),
        orchestratorCompleted()
    );

    executor.execute(new ArrayList<>(), newEvents, propagatedHistoryProto);

    // Verify the propagated history was surfaced on the context
    assertTrue(captured[0].isPresent());
    PropagatedHistory history = captured[0].get();

    assertEquals(HistoryPropagationScope.LINEAGE, history.getScope());
    assertEquals(1, history.getEvents().size());
    assertEquals("ValidateCard", history.getEvents().get(0).getTaskScheduled().getName());

    assertEquals(1, history.getWorkflows().size());
    WorkflowResult wf = history.getWorkflows().get(0);
    assertEquals("payment-app", wf.getAppId());
    assertEquals("ProcessPayment", wf.getName());
    assertEquals("parent-instance-1", wf.getInstanceId());
    assertTrue(wf.getLastActivityByName("ValidateCard").isPresent());
  }

  @Test
  void execute_withoutPropagatedHistory_returnsEmptyOptional() {
    final String orchestratorName = "ChildOrchestrator";
    final Optional<PropagatedHistory>[] captured = new Optional[]{Optional.empty()};

    TaskOrchestration orchestration = ctx -> {
      captured[0] = ctx.getPropagatedHistory();
      ctx.complete(null);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration);

    List<HistoryEvents.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "child-1", "\"input\""),
        orchestratorCompleted()
    );

    executor.execute(new ArrayList<>(), newEvents);

    assertFalse(captured[0].isPresent());
  }

  // ==================================================================================
  // Tests for PropagatedHistory query methods
  // ==================================================================================

  @Test
  void propagatedHistory_getAppIDs_returnsDeduplicated() {
    HistoryEvents.HistoryEvent event1 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("A").build())
        .build();
    HistoryEvents.HistoryEvent event2 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("B").build())
        .build();

    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event1.toByteString())
            .setInstanceId("inst1")
            .setWorkflowName("WF1")
            .build())
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event2.toByteString())
            .setInstanceId("inst2")
            .setWorkflowName("WF2")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    List<String> appIds = history.getAppIDs();
    assertEquals(1, appIds.size());
    assertEquals("app1", appIds.get(0));
  }

  @Test
  void propagatedHistory_getLastWorkflowByName_returnsLastMatch() {
    HistoryEvents.HistoryEvent event1 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("A").build())
        .build();
    HistoryEvents.HistoryEvent event2 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("B").build())
        .build();

    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event1.toByteString())
            .setInstanceId("inst1")
            .setWorkflowName("ProcessPayment")
            .build())
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app2")
            .addRawEvents(event2.toByteString())
            .setInstanceId("inst2")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    Optional<WorkflowResult> result = history.getLastWorkflowByName("ProcessPayment");
    assertTrue(result.isPresent());
    assertEquals("inst2", result.get().getInstanceId());
    assertEquals("app2", result.get().getAppId());
  }

  @Test
  void propagatedHistory_getLastWorkflowByName_returnsEmptyForMissing() {
    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .setInstanceId("inst1")
            .setWorkflowName("WF1")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    Optional<WorkflowResult> result = history.getLastWorkflowByName("NonExistent");
    assertFalse(result.isPresent());
  }

  @Test
  void propagatedHistory_getEventsByAppID_filtersCorrectly() {
    HistoryEvents.HistoryEvent event1 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("A").build())
        .build();
    HistoryEvents.HistoryEvent event2 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("B").build())
        .build();
    HistoryEvents.HistoryEvent event3 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(2)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1002).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("C").build())
        .build();

    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event1.toByteString())
            .addRawEvents(event2.toByteString())
            .setInstanceId("inst1")
            .setWorkflowName("WF1")
            .build())
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app2")
            .addRawEvents(event3.toByteString())
            .setInstanceId("inst2")
            .setWorkflowName("WF2")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    List<HistoryEvents.HistoryEvent> app1Events = history.getEventsByAppID("app1");
    assertEquals(2, app1Events.size());
    assertEquals("A", app1Events.get(0).getTaskScheduled().getName());
    assertEquals("B", app1Events.get(1).getTaskScheduled().getName());

    List<HistoryEvents.HistoryEvent> app2Events = history.getEventsByAppID("app2");
    assertEquals(1, app2Events.size());
    assertEquals("C", app2Events.get(0).getTaskScheduled().getName());
  }

  @Test
  void propagatedHistory_getEventsByInstanceID_filtersCorrectly() {
    HistoryEvents.HistoryEvent event1 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("A").build())
        .build();
    HistoryEvents.HistoryEvent event2 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("B").build())
        .build();

    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event1.toByteString())
            .setInstanceId("inst-abc")
            .setWorkflowName("WF1")
            .build())
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event2.toByteString())
            .setInstanceId("inst-xyz")
            .setWorkflowName("WF2")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    List<HistoryEvents.HistoryEvent> events = history.getEventsByInstanceID("inst-abc");
    assertEquals(1, events.size());
    assertEquals("A", events.get(0).getTaskScheduled().getName());
  }

  @Test
  void propagatedHistory_getEventsByWorkflowName_filtersCorrectly() {
    HistoryEvents.HistoryEvent event1 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder().setName("A").build())
        .build();

    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("app1")
            .addRawEvents(event1.toByteString())
            .setInstanceId("inst1")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    PropagatedHistory history = PropagatedHistory.fromProto(proto);

    List<HistoryEvents.HistoryEvent> events = history.getEventsByWorkflowName("ProcessPayment");
    assertEquals(1, events.size());
    assertEquals("A", events.get(0).getTaskScheduled().getName());

    List<HistoryEvents.HistoryEvent> empty = history.getEventsByWorkflowName("NonExistent");
    assertTrue(empty.isEmpty());
  }

  // ==================================================================================
  // Tests for HistoryPropagationScope enum
  // ==================================================================================

  @Test
  void historyPropagationScope_toProto_convertsCorrectly() {
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_NONE,
        HistoryPropagationScope.NONE.toProto());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY,
        HistoryPropagationScope.OWN_HISTORY.toProto());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        HistoryPropagationScope.LINEAGE.toProto());
  }

  @Test
  void historyPropagationScope_fromProto_convertsCorrectly() {
    assertEquals(HistoryPropagationScope.NONE,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_NONE));
    assertEquals(HistoryPropagationScope.OWN_HISTORY,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY));
    assertEquals(HistoryPropagationScope.LINEAGE,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE));
  }

  // ==================================================================================
  // Tests for TaskOptions builder with historyPropagationScope
  // ==================================================================================

  @Test
  void taskOptions_builder_setsHistoryPropagationScope() {
    TaskOptions options = TaskOptions.builder()
        .historyPropagationScope(HistoryPropagationScope.LINEAGE)
        .appID("myApp")
        .build();

    assertEquals(HistoryPropagationScope.LINEAGE, options.getHistoryPropagationScope());
    assertTrue(options.hasHistoryPropagationScope());
    assertEquals("myApp", options.getAppID());
  }

  @Test
  void taskOptions_builder_withoutScope_hasNoHistoryPropagationScope() {
    TaskOptions options = TaskOptions.builder()
        .appID("myApp")
        .build();

    assertNull(options.getHistoryPropagationScope());
    assertFalse(options.hasHistoryPropagationScope());
  }

  @Test
  void taskOptions_noneScope_isNotConsideredSet() {
    TaskOptions options = TaskOptions.builder()
        .historyPropagationScope(HistoryPropagationScope.NONE)
        .build();

    assertEquals(HistoryPropagationScope.NONE, options.getHistoryPropagationScope());
    assertFalse(options.hasHistoryPropagationScope());
  }

  // ==================================================================================
  // Tests for WorkflowResult typed lookups
  // ==================================================================================

  private static HistoryEvents.HistoryEvent taskScheduled(int eventId, String name, String input) {
    HistoryEvents.TaskScheduledEvent.Builder ts = HistoryEvents.TaskScheduledEvent.newBuilder()
        .setName(name);
    if (input != null) {
      ts.setInput(StringValue.of(input));
    }
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setTaskScheduled(ts.build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskCompleted(int scheduledId, String result) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(scheduledId + 100)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setTaskCompleted(HistoryEvents.TaskCompletedEvent.newBuilder()
            .setTaskScheduledId(scheduledId)
            .setResult(StringValue.of(result))
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent taskFailed(int scheduledId, String errorType) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(scheduledId + 200)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1002).build())
        .setTaskFailed(HistoryEvents.TaskFailedEvent.newBuilder()
            .setTaskScheduledId(scheduledId)
            .setFailureDetails(Orchestration.TaskFailureDetails.newBuilder()
                .setErrorType(errorType)
                .build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childCreated(int eventId, String name) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(eventId)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setChildWorkflowInstanceCreated(HistoryEvents.ChildWorkflowInstanceCreatedEvent.newBuilder()
            .setName(name)
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childCompleted(int scheduledId, String result) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(scheduledId + 100)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1001).build())
        .setChildWorkflowInstanceCompleted(HistoryEvents.ChildWorkflowInstanceCompletedEvent.newBuilder()
            .setTaskScheduledId(scheduledId)
            .setResult(StringValue.of(result))
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent childFailed(int scheduledId, String errorType) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(scheduledId + 200)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1002).build())
        .setChildWorkflowInstanceFailed(HistoryEvents.ChildWorkflowInstanceFailedEvent.newBuilder()
            .setTaskScheduledId(scheduledId)
            .setFailureDetails(Orchestration.TaskFailureDetails.newBuilder()
                .setErrorType(errorType)
                .build())
            .build())
        .build();
  }

  private static PropagatedHistory historyWithChunkEvents(String workflowName,
      HistoryEvents.HistoryEvent... events) {
    HistoryEvents.PropagatedHistoryChunk.Builder chunk = HistoryEvents.PropagatedHistoryChunk.newBuilder()
        .setAppId("app1")
        .setInstanceId("inst1")
        .setWorkflowName(workflowName);
    for (HistoryEvents.HistoryEvent e : events) {
      chunk.addRawEvents(e.toByteString());
    }
    HistoryEvents.PropagatedHistory proto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(chunk.build())
        .build();
    return PropagatedHistory.fromProto(proto);
  }

  @Test
  void workflowResult_getLastActivityByName_returnsCompletedResult() {
    PropagatedHistory history = historyWithChunkEvents("ProcessPayment",
        taskScheduled(1, "ValidateCard", "\"4111\""),
        taskCompleted(1, "\"OK\""));

    WorkflowResult wf = history.getLastWorkflowByName("ProcessPayment").get();
    Optional<ActivityResult> activity = wf.getLastActivityByName("ValidateCard");
    assertTrue(activity.isPresent());
    assertEquals("ValidateCard", activity.get().getName());
    assertTrue(activity.get().isStarted());
    assertTrue(activity.get().isCompleted());
    assertFalse(activity.get().isFailed());
    assertEquals("\"4111\"", activity.get().getInput().getValue());
    assertEquals("\"OK\"", activity.get().getOutput().getValue());
    assertNull(activity.get().getError());
  }

  @Test
  void workflowResult_getLastActivityByName_returnsFailedResult() {
    PropagatedHistory history = historyWithChunkEvents("ProcessPayment",
        taskScheduled(1, "ValidateCard", null),
        taskFailed(1, "ValidationError"));

    WorkflowResult wf = history.getLastWorkflowByName("ProcessPayment").get();
    ActivityResult activity = wf.getLastActivityByName("ValidateCard").get();
    assertTrue(activity.isStarted());
    assertFalse(activity.isCompleted());
    assertTrue(activity.isFailed());
    assertEquals("ValidationError", activity.getError().getErrorType());
  }

  @Test
  void workflowResult_getLastActivityByName_returnsStartedOnly() {
    PropagatedHistory history = historyWithChunkEvents("ProcessPayment",
        taskScheduled(1, "ValidateCard", null));

    WorkflowResult wf = history.getLastWorkflowByName("ProcessPayment").get();
    ActivityResult activity = wf.getLastActivityByName("ValidateCard").get();
    assertTrue(activity.isStarted());
    assertFalse(activity.isCompleted());
    assertFalse(activity.isFailed());
  }

  @Test
  void workflowResult_getLastActivityByName_returnsEmptyForMissing() {
    PropagatedHistory history = historyWithChunkEvents("ProcessPayment",
        taskScheduled(1, "ValidateCard", null));

    WorkflowResult wf = history.getLastWorkflowByName("ProcessPayment").get();
    assertFalse(wf.getLastActivityByName("NotThere").isPresent());
  }

  @Test
  void workflowResult_getActivitiesByName_returnsAllInOrder() {
    PropagatedHistory history = historyWithChunkEvents("ProcessPayment",
        taskScheduled(1, "ValidateCard", "\"a\""),
        taskCompleted(1, "\"first\""),
        taskScheduled(2, "ValidateCard", "\"b\""),
        taskCompleted(2, "\"second\""));

    WorkflowResult wf = history.getLastWorkflowByName("ProcessPayment").get();
    List<ActivityResult> all = wf.getActivitiesByName("ValidateCard");
    assertEquals(2, all.size());
    assertEquals("\"first\"", all.get(0).getOutput().getValue());
    assertEquals("\"second\"", all.get(1).getOutput().getValue());

    // Last == last element of plural.
    assertEquals("\"second\"", wf.getLastActivityByName("ValidateCard").get().getOutput().getValue());
  }

  @Test
  void workflowResult_getLastChildWorkflowByName_returnsCompletedResult() {
    PropagatedHistory history = historyWithChunkEvents("ParentFlow",
        childCreated(1, "ChildFlow"),
        childCompleted(1, "\"done\""));

    WorkflowResult wf = history.getLastWorkflowByName("ParentFlow").get();
    Optional<ChildWorkflowResult> child = wf.getLastChildWorkflowByName("ChildFlow");
    assertTrue(child.isPresent());
    assertEquals("ChildFlow", child.get().getName());
    assertTrue(child.get().isStarted());
    assertTrue(child.get().isCompleted());
    assertFalse(child.get().isFailed());
    assertEquals("\"done\"", child.get().getOutput().getValue());
  }

  @Test
  void workflowResult_getLastChildWorkflowByName_returnsFailedResult() {
    PropagatedHistory history = historyWithChunkEvents("ParentFlow",
        childCreated(1, "ChildFlow"),
        childFailed(1, "ChildBoom"));

    WorkflowResult wf = history.getLastWorkflowByName("ParentFlow").get();
    ChildWorkflowResult child = wf.getLastChildWorkflowByName("ChildFlow").get();
    assertTrue(child.isFailed());
    assertEquals("ChildBoom", child.getError().getErrorType());
  }

  @Test
  void workflowResult_getChildWorkflowsByName_returnsAllInOrder() {
    PropagatedHistory history = historyWithChunkEvents("ParentFlow",
        childCreated(1, "ChildFlow"),
        childCompleted(1, "\"first\""),
        childCreated(2, "ChildFlow"),
        childCompleted(2, "\"second\""));

    WorkflowResult wf = history.getLastWorkflowByName("ParentFlow").get();
    List<ChildWorkflowResult> all = wf.getChildWorkflowsByName("ChildFlow");
    assertEquals(2, all.size());
    assertEquals("\"first\"", all.get(0).getOutput().getValue());
    assertEquals("\"second\"", all.get(1).getOutput().getValue());
  }

  @Test
  void workflowResult_getLastChildWorkflowByName_returnsEmptyForMissing() {
    PropagatedHistory history = historyWithChunkEvents("ParentFlow",
        childCreated(1, "ChildFlow"));

    WorkflowResult wf = history.getLastWorkflowByName("ParentFlow").get();
    assertFalse(wf.getLastChildWorkflowByName("NotThere").isPresent());
  }
}
