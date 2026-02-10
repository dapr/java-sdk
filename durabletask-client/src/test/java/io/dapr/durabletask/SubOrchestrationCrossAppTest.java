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

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for sub-orchestration cross-app routing in TaskOrchestrationExecutor.
 */
class SubOrchestrationCrossAppTest {

  private static final Logger logger = Logger.getLogger(SubOrchestrationCrossAppTest.class.getName());
  private static final Duration MAX_TIMER_INTERVAL = Duration.ofDays(3);

  /**
   * Helper to build an OrchestratorStarted history event.
   */
  private static OrchestratorService.HistoryEvent orchestratorStarted() {
    return OrchestratorService.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setOrchestratorStarted(OrchestratorService.OrchestratorStartedEvent.newBuilder().build())
        .build();
  }

  /**
   * Helper to build an ExecutionStarted history event with a router.
   */
  private static OrchestratorService.HistoryEvent executionStarted(
      String name, String instanceId, String input, OrchestratorService.TaskRouter router) {
    OrchestratorService.ExecutionStartedEvent.Builder esBuilder = OrchestratorService.ExecutionStartedEvent
        .newBuilder()
        .setName(name)
        .setOrchestrationInstance(
            OrchestratorService.OrchestrationInstance.newBuilder().setInstanceId(instanceId).build())
        .setInput(StringValue.of(input));

    OrchestratorService.HistoryEvent.Builder builder = OrchestratorService.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setExecutionStarted(esBuilder.build());

    if (router != null) {
      builder.setRouter(router);
    }

    return builder.build();
  }

  /**
   * Helper to build an OrchestratorCompleted history event.
   */
  private static OrchestratorService.HistoryEvent orchestratorCompleted() {
    return OrchestratorService.HistoryEvent.newBuilder()
        .setEventId(-1)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1000).build())
        .setOrchestratorCompleted(OrchestratorService.OrchestratorCompletedEvent.newBuilder().build())
        .build();
  }

  /**
   * Creates a TaskOrchestrationExecutor with the given orchestrator and app ID.
   */
  private TaskOrchestrationExecutor createExecutor(String orchestratorName, TaskOrchestration orchestration,
      String appId) {
    HashMap<String, TaskOrchestrationFactory> factories = new HashMap<>();
    factories.put(orchestratorName, new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return orchestratorName;
      }

      @Override
      public TaskOrchestration create() {
        return orchestration;
      }
    });
    return new TaskOrchestrationExecutor(factories, new JacksonDataConverter(), MAX_TIMER_INTERVAL, logger, appId);
  }

  // ==================================================================================
  // Tests for callSubOrchestrator with cross-app routing
  // ==================================================================================

  @Test
  void callSubOrchestrator_withTargetAppId_setsRouterOnAction() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "ChildOrchestrator";
    final String sourceAppId = "app1";
    final String targetAppId = "app2";

    // The orchestrator calls a sub-orchestration with a target app ID
    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.withAppID(targetAppId);
      ctx.callSubOrchestrator(subOrchestratorName, "input", "child-instance-1", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"hello\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    // There should be a CreateSubOrchestration action
    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCreateSubOrchestration());

    // Verify the CreateSubOrchestrationAction has the router
    OrchestratorService.CreateSubOrchestrationAction createSub = action.getCreateSubOrchestration();
    assertEquals(subOrchestratorName, createSub.getName());
    assertEquals("child-instance-1", createSub.getInstanceId());
    assertTrue(createSub.hasRouter());
    assertEquals(sourceAppId, createSub.getRouter().getSourceAppID());
    assertTrue(createSub.getRouter().hasTargetAppID());
    assertEquals(targetAppId, createSub.getRouter().getTargetAppID());

    // Verify the OrchestratorAction also has the router
    assertTrue(action.hasRouter());
    assertEquals(sourceAppId, action.getRouter().getSourceAppID());
    assertTrue(action.getRouter().hasTargetAppID());
    assertEquals(targetAppId, action.getRouter().getTargetAppID());
  }

  @Test
  void callSubOrchestrator_withoutTargetAppId_setsRouterWithSourceOnly() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "ChildOrchestrator";
    final String sourceAppId = "app1";

    // The orchestrator calls a sub-orchestration WITHOUT a target app ID
    TaskOrchestration orchestration = ctx -> {
      ctx.callSubOrchestrator(subOrchestratorName, "input", "child-instance-1", null, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"hello\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCreateSubOrchestration());

    // Router should have source only, no target
    OrchestratorService.CreateSubOrchestrationAction createSub = action.getCreateSubOrchestration();
    assertTrue(createSub.hasRouter());
    assertEquals(sourceAppId, createSub.getRouter().getSourceAppID());
    assertFalse(createSub.getRouter().hasTargetAppID());

    // OrchestratorAction router should also have source only
    assertTrue(action.hasRouter());
    assertEquals(sourceAppId, action.getRouter().getSourceAppID());
    assertFalse(action.getRouter().hasTargetAppID());
  }

  @Test
  void callSubOrchestrator_withNullAppId_noRouterSet() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "ChildOrchestrator";

    // The orchestrator calls a sub-orchestration with no app routing context
    TaskOrchestration orchestration = ctx -> {
      ctx.callSubOrchestrator(subOrchestratorName, "input", "child-instance-1", null, String.class);
    };

    // Create executor with null appId (no router context)
    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, null);

    // ExecutionStarted without a router
    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"hello\"", null),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCreateSubOrchestration());

    // No router should be set when appId is null
    OrchestratorService.CreateSubOrchestrationAction createSub = action.getCreateSubOrchestration();
    assertFalse(createSub.hasRouter());
    assertFalse(action.hasRouter());
  }

  // ==================================================================================
  // Tests for EXECUTIONSTARTED event router processing (appId extraction)
  // ==================================================================================

  @Test
  void executionStarted_withRouterTargetAppId_usesTargetAsAppId() {
    final String orchestratorName = "SubOrchestrator";
    final String sourceAppId = "parent-app";
    final String targetAppId = "child-app";

    // This orchestrator will call a local sub-orchestrator with no target app; the router source
    // on that sub-action should be the target app id we extracted from the event router
    final String[] capturedAppId = new String[1];
    TaskOrchestration orchestration = ctx -> {
      capturedAppId[0] = ctx.getAppId();
      ctx.complete(null);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    // Router with BOTH source and target (cross-app suborchestration scenario)
    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .setTargetAppID(targetAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "sub-instance-1", "\"data\"", router),
        orchestratorCompleted()
    );

    executor.execute(new ArrayList<>(), newEvents);

    // The appId should be the target, not the source
    assertEquals(targetAppId, capturedAppId[0]);
  }

  @Test
  void executionStarted_withRouterSourceOnly_usesSourceAsAppId() {
    final String orchestratorName = "MyOrchestrator";
    final String sourceAppId = "my-app";

    final String[] capturedAppId = new String[1];
    TaskOrchestration orchestration = ctx -> {
      capturedAppId[0] = ctx.getAppId();
      ctx.complete(null);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    // Router with source only (normal, single-app scenario)
    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"data\"", router),
        orchestratorCompleted()
    );

    executor.execute(new ArrayList<>(), newEvents);

    assertEquals(sourceAppId, capturedAppId[0]);
  }

  @Test
  void executionStarted_withNoRouter_appIdIsNull() {
    final String orchestratorName = "MyOrchestrator";

    final String[] capturedAppId = new String[]{" sentinel "};
    TaskOrchestration orchestration = ctx -> {
      capturedAppId[0] = ctx.getAppId();
      ctx.complete(null);
    };

    // Executor created with null appId
    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, null);

    // No router on the event
    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"data\"", null),
        orchestratorCompleted()
    );

    executor.execute(new ArrayList<>(), newEvents);

    // appId should remain null since no router was present
    assertNull(capturedAppId[0]);
  }

  // ==================================================================================
  // Tests for completion action router
  // ==================================================================================

  @Test
  void completeOrchestration_withAppId_setsRouterOnCompletionAction() {
    final String orchestratorName = "MyOrchestrator";
    final String appId = "my-app";

    // Orchestrator that completes immediately with a result
    TaskOrchestration orchestration = ctx -> {
      ctx.complete("done");
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, appId);

    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(appId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"input\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCompleteOrchestration());
    assertEquals(OrchestratorService.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED,
        action.getCompleteOrchestration().getOrchestrationStatus());

    // The completion action should have a router with source appId
    assertTrue(action.hasRouter());
    assertEquals(appId, action.getRouter().getSourceAppID());
    assertFalse(action.getRouter().hasTargetAppID());
  }

  @Test
  void completeOrchestration_withNullAppId_noRouterOnCompletionAction() {
    final String orchestratorName = "MyOrchestrator";

    TaskOrchestration orchestration = ctx -> {
      ctx.complete("done");
    };

    // Executor with null appId
    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, null);

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "instance-1", "\"input\"", null),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCompleteOrchestration());

    // No router should be set
    assertFalse(action.hasRouter());
  }

  @Test
  void completeOrchestration_crossAppSubOrchestrator_routerHasTargetDerivedAppId() {
    final String orchestratorName = "SubOrchestrator";
    final String parentAppId = "parent-app";
    final String targetAppId = "child-app";

    // Simulates a cross-app sub-orchestrator that receives a router with target
    TaskOrchestration orchestration = ctx -> {
      ctx.complete("sub-result");
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, parentAppId);

    // Router has both source and target (cross-app suborchestration)
    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(parentAppId)
        .setTargetAppID(targetAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "sub-instance-1", "\"input\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCompleteOrchestration());

    // The router source should be the target app (since that's where we're executing)
    assertTrue(action.hasRouter());
    assertEquals(targetAppId, action.getRouter().getSourceAppID());
  }

  // ==================================================================================
  // Tests for combined suborchestration + completion routing
  // ==================================================================================

  @Test
  void crossAppSubOrchestration_fullFlow_routersCorrectlySet() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "RemoteChild";
    final String sourceAppId = "app1";
    final String targetAppId = "app2";

    // Parent orchestrator calls a cross-app sub-orchestration and then completes
    TaskOrchestration orchestration = ctx -> {
      TaskOptions options = TaskOptions.withAppID(targetAppId);
      ctx.callSubOrchestrator(subOrchestratorName, "data", "child-id-1", options, String.class);
      // Note: orchestrator will yield here waiting for the sub-orchestration to complete
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"start\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    // Should have 1 action: CreateSubOrchestration
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction subAction = actions.get(0);
    assertTrue(subAction.hasCreateSubOrchestration());

    OrchestratorService.CreateSubOrchestrationAction createSub = subAction.getCreateSubOrchestration();
    assertEquals(subOrchestratorName, createSub.getName());
    assertEquals("child-id-1", createSub.getInstanceId());

    // Verify cross-app router on the sub-orchestration action
    assertTrue(createSub.hasRouter());
    assertEquals(sourceAppId, createSub.getRouter().getSourceAppID());
    assertEquals(targetAppId, createSub.getRouter().getTargetAppID());

    // Verify cross-app router on the OrchestratorAction envelope
    assertTrue(subAction.hasRouter());
    assertEquals(sourceAppId, subAction.getRouter().getSourceAppID());
    assertEquals(targetAppId, subAction.getRouter().getTargetAppID());
  }

  @Test
  void callSubOrchestrator_withEmptyAppId_noRouterSet() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "ChildOrchestrator";

    TaskOrchestration orchestration = ctx -> {
      ctx.callSubOrchestrator(subOrchestratorName, "input", "child-1", null, String.class);
    };

    // Executor created with empty appId
    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, "");

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"hello\"", null),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertEquals(1, actions.size());

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCreateSubOrchestration());

    // No router should be set when appId is empty
    assertFalse(action.getCreateSubOrchestration().hasRouter());
    assertFalse(action.hasRouter());
  }

  @Test
  void callSubOrchestrator_withRetryPolicyAndAppId_setsRouterAndRetries() {
    final String orchestratorName = "ParentOrchestrator";
    final String subOrchestratorName = "ChildOrchestrator";
    final String sourceAppId = "app1";
    final String targetAppId = "app2";

    TaskOrchestration orchestration = ctx -> {
      RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ofSeconds(1));
      TaskOptions options = TaskOptions.builder()
          .retryPolicy(retryPolicy)
          .appID(targetAppId)
          .build();
      ctx.callSubOrchestrator(subOrchestratorName, "input", "child-1", options, String.class);
    };

    TaskOrchestrationExecutor executor = createExecutor(orchestratorName, orchestration, sourceAppId);

    OrchestratorService.TaskRouter router = OrchestratorService.TaskRouter.newBuilder()
        .setSourceAppID(sourceAppId)
        .build();

    List<OrchestratorService.HistoryEvent> newEvents = List.of(
        orchestratorStarted(),
        executionStarted(orchestratorName, "parent-instance", "\"hello\"", router),
        orchestratorCompleted()
    );

    TaskOrchestratorResult result = executor.execute(new ArrayList<>(), newEvents);

    // With RetriableTask the first attempt creates the action; we should still see
    // the sub-orchestration action with cross-app routing
    List<OrchestratorService.OrchestratorAction> actions = new ArrayList<>(result.getActions());
    assertTrue(actions.size() >= 1);

    OrchestratorService.OrchestratorAction action = actions.get(0);
    assertTrue(action.hasCreateSubOrchestration());

    OrchestratorService.CreateSubOrchestrationAction createSub = action.getCreateSubOrchestration();
    assertTrue(createSub.hasRouter());
    assertEquals(sourceAppId, createSub.getRouter().getSourceAppID());
    assertEquals(targetAppId, createSub.getRouter().getTargetAppID());
  }
}
