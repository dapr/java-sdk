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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for the full workflow history propagation flow.
 * Tests the end-to-end behavior of parent workflows setting propagation scope
 * on actions, and child workflows/activities receiving propagated history.
 */
class HistoryPropagationIntegrationTest {

  private static final Logger logger = Logger.getLogger(HistoryPropagationIntegrationTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);
  private static final Instant TEST_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

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

  private static HistoryEvents.HistoryEvent executionStarted(String name, String instanceId) {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(name)
            .setWorkflowInstance(
                Orchestration.WorkflowInstance.newBuilder().setInstanceId(instanceId).build())
            .build())
        .build();
  }

  private static HistoryEvents.HistoryEvent orchestratorCompleted() {
    return HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setWorkflowCompleted(HistoryEvents.WorkflowCompletedEvent.newBuilder().build())
        .build();
  }

  private TaskOrchestrationExecutor createExecutor(String name, TaskOrchestration orchestration, String appId) {
    TaskOrchestrationFactories factories = new TaskOrchestrationFactories();
    factories.addOrchestration(new TaskOrchestrationFactory() {
      @Override
      public String getName() { return name; }
      @Override
      public TaskOrchestration create() { return orchestration; }
      @Override
      public String getVersionName() { return null; }
      @Override
      public Boolean isLatestVersion() { return false; }
    });
    return new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, logger, appId);
  }

  /**
   * Integration test: Parent workflow with Lineage scope →
   * child workflow receives full history including parent's events.
   *
   * This simulates the full flow:
   * 1. Parent orchestrates and sets LINEAGE scope on child workflow call
   * 2. The runtime (simulated) builds PropagatedHistory from parent's history
   * 3. Child workflow receives and can query the propagated history
   */
  @Test
  void lineageScope_parentToChild_childReceivesParentEvents() {
    // === PHASE 1: Parent workflow schedules child with LINEAGE scope ===
    final String parentName = "ProcessPayment";
    final String childName = "FraudDetection";

    TaskOrchestration parentOrchestration = ctx -> {
      TaskOptions opts = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.LINEAGE)
          .build();
      ctx.callSubOrchestrator(childName, "payment-data", "child-inst-1", opts, String.class);
    };

    TaskOrchestrationExecutor parentExecutor = createExecutor(parentName, parentOrchestration, "payment-app");

    List<HistoryEvents.HistoryEvent> parentNewEvents = List.of(
        workflowStarted(),
        executionStarted(parentName, "parent-inst-1"),
        orchestratorCompleted()
    );

    TaskOrchestratorResult parentResult = parentExecutor.execute(new ArrayList<>(), parentNewEvents);
    List<OrchestratorActions.WorkflowAction> parentActions = new ArrayList<>(parentResult.getActions());

    // Verify parent set LINEAGE scope on the CreateChildWorkflowAction
    assertEquals(1, parentActions.size());
    OrchestratorActions.CreateChildWorkflowAction createChild = parentActions.get(0).getCreateChildWorkflow();
    assertTrue(createChild.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        createChild.getHistoryPropagationScope());

    // === PHASE 2: Simulate runtime building propagated history from parent ===
    // In a real scenario, the Dapr runtime builds this from the parent's execution history
    HistoryEvents.HistoryEvent parentTaskScheduled = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("ValidateCard")
            .build())
        .build();
    HistoryEvents.HistoryEvent parentTaskCompleted = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskCompleted(HistoryEvents.TaskCompletedEvent.newBuilder()
            .setTaskScheduledId(0)
            .setResult(StringValue.of("\"valid\""))
            .build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistory = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("payment-app")
            .addRawEvents(parentTaskScheduled.toByteString())
            .addRawEvents(parentTaskCompleted.toByteString())
            .setInstanceId("parent-inst-1")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    // === PHASE 3: Child workflow receives and queries propagated history ===
    final Optional<PropagatedHistory>[] childHistory = new Optional[]{Optional.empty()};

    TaskOrchestration childOrchestration = ctx -> {
      childHistory[0] = ctx.getPropagatedHistory();
      ctx.complete("fraud-check-passed");
    };

    TaskOrchestrationExecutor childExecutor = createExecutor(childName, childOrchestration, "fraud-app");

    List<HistoryEvents.HistoryEvent> childNewEvents = List.of(
        workflowStarted(),
        executionStarted(childName, "child-inst-1"),
        orchestratorCompleted()
    );

    childExecutor.execute(new ArrayList<>(), childNewEvents, propagatedHistory);

    // Verify child received the propagated history
    assertTrue(childHistory[0].isPresent());
    PropagatedHistory history = childHistory[0].get();
    assertEquals(HistoryPropagationScope.LINEAGE, history.getScope());
    assertEquals(2, history.getEvents().size());

    // Verify child can query by workflow name
    Optional<WorkflowResult> parentWf = history.getLastWorkflowByName("ProcessPayment");
    assertTrue(parentWf.isPresent());
    assertEquals("payment-app", parentWf.get().getAppId());
    assertEquals("parent-inst-1", parentWf.get().getInstanceId());

    // Typed activity lookup on the parent workflow
    Optional<ActivityResult> validate = parentWf.get().getLastActivityByName("ValidateCard");
    assertTrue(validate.isPresent());
    assertTrue(validate.get().isCompleted());
    assertEquals("\"valid\"", validate.get().getOutput().getValue());

    // Verify child can filter events by app ID
    List<HistoryEvents.HistoryEvent> paymentAppEvents = history.getEventsByAppID("payment-app");
    assertEquals(2, paymentAppEvents.size());

    // Verify deduplicated app IDs
    List<String> appIds = history.getAppIDs();
    assertEquals(1, appIds.size());
    assertEquals("payment-app", appIds.get(0));
  }

  /**
   * Integration test: Parent workflow with OWN_HISTORY scope →
   * child workflow only receives caller's events (trust boundary).
   * Grandparent history is NOT forwarded.
   */
  @Test
  void ownHistoryScope_parentToChild_childReceivesOnlyCallerEvents() {
    // === PHASE 1: Parent workflow schedules child with OWN_HISTORY scope ===
    final String parentName = "SettlePayment";
    final String childName = "RecordTransaction";

    TaskOrchestration parentOrchestration = ctx -> {
      TaskOptions opts = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.OWN_HISTORY)
          .build();
      ctx.callSubOrchestrator(childName, "settlement-data", "child-inst-2", opts, String.class);
    };

    TaskOrchestrationExecutor parentExecutor = createExecutor(parentName, parentOrchestration, "settlement-app");

    List<HistoryEvents.HistoryEvent> parentNewEvents = List.of(
        workflowStarted(),
        executionStarted(parentName, "parent-inst-2"),
        orchestratorCompleted()
    );

    TaskOrchestratorResult parentResult = parentExecutor.execute(new ArrayList<>(), parentNewEvents);
    List<OrchestratorActions.WorkflowAction> parentActions = new ArrayList<>(parentResult.getActions());

    // Verify parent set OWN_HISTORY scope
    OrchestratorActions.CreateChildWorkflowAction createChild = parentActions.get(0).getCreateChildWorkflow();
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY,
        createChild.getHistoryPropagationScope());

    // === PHASE 2: Simulate runtime building OWN_HISTORY propagated history ===
    // Only the caller's (parent's) events - no grandparent chain
    HistoryEvents.HistoryEvent parentEvent = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("ApproveSettlement")
            .build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistory = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("settlement-app")
            .addRawEvents(parentEvent.toByteString())
            .setInstanceId("parent-inst-2")
            .setWorkflowName("SettlePayment")
            .build())
        .build();

    // === PHASE 3: Child receives only OWN_HISTORY (no grandparent) ===
    final Optional<PropagatedHistory>[] childHistory = new Optional[]{Optional.empty()};

    TaskOrchestration childOrchestration = ctx -> {
      childHistory[0] = ctx.getPropagatedHistory();
      ctx.complete("recorded");
    };

    TaskOrchestrationExecutor childExecutor = createExecutor(childName, childOrchestration, "record-app");

    List<HistoryEvents.HistoryEvent> childNewEvents = List.of(
        workflowStarted(),
        executionStarted(childName, "child-inst-2"),
        orchestratorCompleted()
    );

    childExecutor.execute(new ArrayList<>(), childNewEvents, propagatedHistory);

    assertTrue(childHistory[0].isPresent());
    PropagatedHistory history = childHistory[0].get();
    assertEquals(HistoryPropagationScope.OWN_HISTORY, history.getScope());

    // Only one chunk from the immediate parent, no grandparent
    assertEquals(1, history.getWorkflows().size());
    assertEquals("SettlePayment", history.getWorkflows().get(0).getName());
    assertEquals(1, history.getEvents().size());
  }

  /**
   * Integration test: Activity receives propagated history.
   * Parent workflow sets LINEAGE scope on activity call → activity can inspect history.
   */
  @Test
  void lineageScope_parentToActivity_activityReceivesHistory() throws Throwable {
    // === PHASE 1: Parent sets LINEAGE scope on activity call ===
    final String parentName = "ProcessPayment";
    final String activityName = "SettlePayment";

    TaskOrchestration parentOrchestration = ctx -> {
      TaskOptions opts = TaskOptions.builder()
          .historyPropagationScope(HistoryPropagationScope.LINEAGE)
          .build();
      ctx.callActivity(activityName, "settle-data", opts, String.class);
    };

    TaskOrchestrationExecutor parentExecutor = createExecutor(parentName, parentOrchestration, "payment-app");

    List<HistoryEvents.HistoryEvent> parentNewEvents = List.of(
        workflowStarted(),
        executionStarted(parentName, "parent-inst-3"),
        orchestratorCompleted()
    );

    TaskOrchestratorResult parentResult = parentExecutor.execute(new ArrayList<>(), parentNewEvents);
    List<OrchestratorActions.WorkflowAction> parentActions = new ArrayList<>(parentResult.getActions());

    // Verify the ScheduleTaskAction has LINEAGE scope
    OrchestratorActions.ScheduleTaskAction scheduleTask = parentActions.get(0).getScheduleTask();
    assertTrue(scheduleTask.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        scheduleTask.getHistoryPropagationScope());

    // === PHASE 2: Simulate runtime building propagated history for activity ===
    HistoryEvents.HistoryEvent event = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("ValidateCard")
            .build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistoryProto = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("payment-app")
            .addRawEvents(event.toByteString())
            .setInstanceId("parent-inst-3")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    // === PHASE 3: Activity receives and queries propagated history ===
    final Optional<PropagatedHistory>[] activityHistory = new Optional[]{Optional.empty()};

    HashMap<String, TaskActivityFactory> factories = new HashMap<>();
    factories.put(activityName, new TaskActivityFactory() {
      @Override
      public String getName() {
        return activityName;
      }

      @Override
      public TaskActivity create() {
        return ctx -> {
          activityHistory[0] = ctx.getPropagatedHistory();
          return "settled";
        };
      }
    });

    TaskActivityExecutor activityExecutor = new TaskActivityExecutor(factories, new JacksonDataConverter(), logger);

    activityExecutor.execute(activityName, "\"settle-data\"", "exec-1", 0, "", propagatedHistoryProto);

    assertTrue(activityHistory[0].isPresent());
    PropagatedHistory history = activityHistory[0].get();
    assertEquals(HistoryPropagationScope.LINEAGE, history.getScope());
    assertEquals(1, history.getEvents().size());
    assertEquals("ValidateCard", history.getEvents().get(0).getTaskScheduled().getName());

    Optional<WorkflowResult> wf = history.getLastWorkflowByName("ProcessPayment");
    assertTrue(wf.isPresent());
    assertEquals("payment-app", wf.get().getAppId());
  }

  /**
   * Integration test: Multi-tier lineage propagation.
   * Grandparent → Parent → Child all propagate with LINEAGE scope.
   * Child should see events from both grandparent and parent via multiple chunks.
   */
  @Test
  void lineageScope_multiTier_childSeesFullAncestorChain() {
    final String childName = "AuditRecord";
    final Optional<PropagatedHistory>[] childHistory = new Optional[]{Optional.empty()};

    TaskOrchestration childOrchestration = ctx -> {
      childHistory[0] = ctx.getPropagatedHistory();
      ctx.complete("audited");
    };

    TaskOrchestrationExecutor childExecutor = createExecutor(childName, childOrchestration, "audit-app");

    // Build multi-tier propagated history (grandparent + parent)
    HistoryEvents.HistoryEvent gpEvent = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(0)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("InitiatePayment")
            .build())
        .build();
    HistoryEvents.HistoryEvent parentEvent = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.newBuilder()
            .setName("ValidateCard")
            .build())
        .build();
    HistoryEvents.HistoryEvent parentEvent2 = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(2)
        .setTimestamp(ts(TEST_INSTANT))
        .setTaskCompleted(HistoryEvents.TaskCompletedEvent.newBuilder()
            .setTaskScheduledId(1)
            .setResult(StringValue.of("\"valid\""))
            .build())
        .build();

    HistoryEvents.PropagatedHistory propagatedHistory = HistoryEvents.PropagatedHistory.newBuilder()
        .setScope(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE)
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("gateway-app")
            .addRawEvents(gpEvent.toByteString())
            .setInstanceId("gp-inst-1")
            .setWorkflowName("GatewayWorkflow")
            .build())
        .addChunks(HistoryEvents.PropagatedHistoryChunk.newBuilder()
            .setAppId("payment-app")
            .addRawEvents(parentEvent.toByteString())
            .addRawEvents(parentEvent2.toByteString())
            .setInstanceId("parent-inst-1")
            .setWorkflowName("ProcessPayment")
            .build())
        .build();

    List<HistoryEvents.HistoryEvent> childNewEvents = List.of(
        workflowStarted(),
        executionStarted(childName, "child-inst-audit"),
        orchestratorCompleted()
    );

    childExecutor.execute(new ArrayList<>(), childNewEvents, propagatedHistory);

    assertTrue(childHistory[0].isPresent());
    PropagatedHistory history = childHistory[0].get();

    // Verify full ancestor chain
    assertEquals(HistoryPropagationScope.LINEAGE, history.getScope());
    assertEquals(3, history.getEvents().size());
    assertEquals(2, history.getWorkflows().size());

    // Verify app IDs in order
    List<String> appIds = history.getAppIDs();
    assertEquals(2, appIds.size());
    assertEquals("gateway-app", appIds.get(0));
    assertEquals("payment-app", appIds.get(1));

    // Query by workflow name
    Optional<WorkflowResult> gp = history.getLastWorkflowByName("GatewayWorkflow");
    assertTrue(gp.isPresent());
    assertEquals("gateway-app", gp.get().getAppId());
    assertTrue(gp.get().getLastActivityByName("InitiatePayment").isPresent());

    Optional<WorkflowResult> parent = history.getLastWorkflowByName("ProcessPayment");
    assertTrue(parent.isPresent());
    assertEquals("payment-app", parent.get().getAppId());
    Optional<ActivityResult> validate = parent.get().getLastActivityByName("ValidateCard");
    assertTrue(validate.isPresent());
    assertTrue(validate.get().isCompleted());
    assertEquals("\"valid\"", validate.get().getOutput().getValue());

    // Query events by instance ID
    List<HistoryEvents.HistoryEvent> parentEvents = history.getEventsByInstanceID("parent-inst-1");
    assertEquals(2, parentEvents.size());
    assertEquals("ValidateCard", parentEvents.get(0).getTaskScheduled().getName());
  }

  /**
   * Integration test: Combining cross-app routing with history propagation.
   * Both features should work together without interference.
   */
  @Test
  void historyPropagation_combinedWithCrossAppRouting_bothWork() {
    final String parentName = "ParentOrchestrator";
    final String childName = "ChildOrchestrator";
    final String sourceAppId = "source-app";
    final String targetAppId = "target-app";

    TaskOrchestration parentOrchestration = ctx -> {
      TaskOptions opts = TaskOptions.builder()
          .appID(targetAppId)
          .historyPropagationScope(HistoryPropagationScope.LINEAGE)
          .build();
      ctx.callSubOrchestrator(childName, "input", "child-inst-combined", opts, String.class);
    };

    TaskOrchestrationExecutor parentExecutor = createExecutor(parentName, parentOrchestration, sourceAppId);

    Orchestration.TaskRouter router = Orchestration.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    HistoryEvents.HistoryEvent execStarted = HistoryEvents.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(ts(TEST_INSTANT))
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.newBuilder()
            .setName(parentName)
            .setWorkflowInstance(
                Orchestration.WorkflowInstance.newBuilder().setInstanceId("parent-combined").build())
            .build())
        .setRouter(router)
        .build();

    List<HistoryEvents.HistoryEvent> parentNewEvents = List.of(
        workflowStarted(),
        execStarted,
        orchestratorCompleted()
    );

    TaskOrchestratorResult parentResult = parentExecutor.execute(new ArrayList<>(), parentNewEvents);
    List<OrchestratorActions.WorkflowAction> actions = new ArrayList<>(parentResult.getActions());
    assertEquals(1, actions.size());

    OrchestratorActions.WorkflowAction action = actions.get(0);
    OrchestratorActions.CreateChildWorkflowAction createChild = action.getCreateChildWorkflow();

    // Verify BOTH cross-app routing and history propagation are set
    assertTrue(createChild.hasRouter());
    assertEquals(sourceAppId, createChild.getRouter().getSourceAppID());
    assertEquals(targetAppId, createChild.getRouter().getTargetAppID());

    assertTrue(createChild.hasHistoryPropagationScope());
    assertEquals(Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE,
        createChild.getHistoryPropagationScope());

    // Verify OrchestratorAction-level router
    assertTrue(action.hasRouter());
    assertEquals(sourceAppId, action.getRouter().getSourceAppID());
    assertEquals(targetAppId, action.getRouter().getTargetAppID());
  }
}
