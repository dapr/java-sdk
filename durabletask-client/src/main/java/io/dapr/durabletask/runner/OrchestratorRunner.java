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

package io.dapr.durabletask.runner;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.TaskOrchestrationExecutor;
import io.dapr.durabletask.TaskOrchestratorResult;
import io.dapr.durabletask.WorkflowHistoryCache;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Tracer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrchestratorRunner extends DurableRunner {
  private static final Logger logger = Logger.getLogger(OrchestratorRunner.class.getPackage().getName());

  private final OrchestratorService.WorkflowRequest orchestratorRequest;
  private final TaskOrchestrationExecutor taskOrchestrationExecutor;
  @Nullable
  private final WorkflowHistoryCache historyCache;

  /**
   * Constructs a new instance of the OrchestratorRunner class.
   *
   * @param workItem                  The work item containing details about the orchestrator task to be executed.
   * @param taskOrchestrationExecutor The executor responsible for running task orchestration logic.
   * @param sidecarClient             The gRPC stub for communication with the Task Hub sidecar service.
   * @param tracer                    An optional tracer used for distributed tracing, can be null.
   * @param historyCache              The per-stream committed-history cache for stateful-history
   *                                  delta work items, or null when the optimization is disabled.
   */
  public OrchestratorRunner(
      OrchestratorService.WorkItem workItem,
      TaskOrchestrationExecutor taskOrchestrationExecutor,
      TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub sidecarClient,
      @Nullable Tracer tracer,
      @Nullable WorkflowHistoryCache historyCache) {

    super(workItem, sidecarClient, tracer);
    this.orchestratorRequest = workItem.getWorkflowRequest();
    this.taskOrchestrationExecutor = taskOrchestrationExecutor;
    this.historyCache = historyCache;
  }

  @Override
  public void run() {
    String instanceId = orchestratorRequest.getInstanceId();

    List<HistoryEvents.HistoryEvent> pastEvents;
    try {
      pastEvents = resolvePastEvents(instanceId);
    } catch (StatusRuntimeException e) {
      // The cache-miss fallback fetch failed and there is no per-item NACK. Abandon this
      // work item so the backend redelivers it (as a full-history send on a future stream)
      // rather than completing the turn with an incomplete history.
      logException(e);
      return;
    }

    TaskOrchestratorResult taskOrchestratorResult = taskOrchestrationExecutor.execute(
        pastEvents,
        orchestratorRequest.getNewEventsList(),
        orchestratorRequest.hasPropagatedHistory()
            ? orchestratorRequest.getPropagatedHistory() : null);

    updateHistoryCache(instanceId, pastEvents, taskOrchestratorResult);

    var versionBuilder = Orchestration.WorkflowVersion.newBuilder();

    if (StringUtils.isNotEmpty(taskOrchestratorResult.getVersion())) {
      versionBuilder.setName(taskOrchestratorResult.getVersion());
    }

    if (taskOrchestratorResult.getPatches() != null) {
      versionBuilder.addAllPatches(taskOrchestratorResult.getPatches());
    }

    OrchestratorService.WorkflowResponse response = OrchestratorService.WorkflowResponse.newBuilder()
        .setInstanceId(orchestratorRequest.getInstanceId())
        .addAllActions(taskOrchestratorResult.getActions())
        .setCustomStatus(StringValue.of(taskOrchestratorResult.getCustomStatus()))
        .setCompletionToken(workItem.getCompletionToken())
        .setVersion(versionBuilder)
        .build();

    try {
      this.sidecarClient.completeOrchestratorTask(response);
      logger.log(Level.FINEST,
          "Completed orchestrator request for instance: {0}",
          orchestratorRequest.getInstanceId());
    } catch (StatusRuntimeException e) {
      this.logException(e);
    }
  }

  /**
   * Reconstructs the full committed history to replay. For a full send it is simply the request's
   * pastEvents; for a delta send (cachedHistory) it is the cached prefix plus the delta, falling
   * back to a GetInstanceHistory fetch on any cache miss.
   */
  List<HistoryEvents.HistoryEvent> resolvePastEvents(String instanceId) {
    if (this.historyCache == null || !orchestratorRequest.hasCachedHistory()) {
      return orchestratorRequest.getPastEventsList();
    }

    List<HistoryEvents.HistoryEvent> cached = this.historyCache.get(instanceId);
    int expected = orchestratorRequest.getCachedHistory().getEventCount();
    if (cached != null && cached.size() == expected) {
      List<HistoryEvents.HistoryEvent> full =
          new ArrayList<>(cached.size() + orchestratorRequest.getPastEventsCount());
      full.addAll(cached);
      full.addAll(orchestratorRequest.getPastEventsList());
      return full;
    }

    // Cache miss: recover the full committed history from the sidecar. NewEvents is applied on
    // top of this by the executor, so only the committed past is needed here.
    OrchestratorService.GetInstanceHistoryResponse historyResponse = this.sidecarClient.getInstanceHistory(
        OrchestratorService.GetInstanceHistoryRequest.newBuilder().setInstanceId(instanceId).build());
    return historyResponse.getEventsList();
  }

  /**
   * Refreshes the per-stream cache after a turn so the next turn can be served as a delta. Caches
   * only the committed history just replayed (never the not-yet-committed NewEvents), and drops the
   * entry once the instance ends. A CompleteWorkflow action covers completed/failed/terminated/
   * continued-as-new; a TerminateWorkflow action targets a different instance and is deliberately
   * not treated as a reset.
   */
  void updateHistoryCache(
      String instanceId,
      List<HistoryEvents.HistoryEvent> pastEvents,
      TaskOrchestratorResult result) {
    if (this.historyCache == null) {
      return;
    }

    boolean ended = result.getActions().stream()
        .anyMatch(OrchestratorActions.WorkflowAction::hasCompleteWorkflow);
    if (ended) {
      this.historyCache.remove(instanceId);
    } else {
      this.historyCache.put(instanceId, pastEvents);
    }
  }
}
