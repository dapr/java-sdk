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

package io.dapr.durabletask.runner;

import io.dapr.durabletask.TaskOrchestratorResult;
import io.dapr.durabletask.WorkflowHistoryCache;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.OrchestratorActions;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Deterministic tests for the worker's history resolution and cache update, driving
 * {@link OrchestratorRunner}'s package-visible helpers directly. The full-send and cache-hit paths
 * make no RPC, so a null sidecar client suffices; the cache-miss fallback (which does call
 * GetInstanceHistory) is covered by the worker-level in-process test.
 */
class OrchestratorRunnerHistoryTest {

  private static List<HistoryEvents.HistoryEvent> events(int count) {
    List<HistoryEvents.HistoryEvent> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(HistoryEvents.HistoryEvent.newBuilder().setEventId(i + 1).build());
    }
    return list;
  }

  private static OrchestratorRunner runner(OrchestratorService.WorkflowRequest request, WorkflowHistoryCache cache) {
    OrchestratorService.WorkItem workItem = OrchestratorService.WorkItem.newBuilder()
        .setWorkflowRequest(request)
        .build();
    return new OrchestratorRunner(workItem, null, null, null, cache);
  }

  private static TaskOrchestratorResult resultWith(OrchestratorActions.WorkflowAction action) {
    return new TaskOrchestratorResult(List.of(action), "", null, null);
  }

  @Test
  void fullSendReturnsRequestPastEvents() {
    OrchestratorService.WorkflowRequest request = OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId("a")
        .addAllPastEvents(events(4))
        .build();
    OrchestratorRunner runner = runner(request, new WorkflowHistoryCache(null, 0, 0));

    assertEquals(4, runner.resolvePastEvents("a").size());
  }

  @Test
  void cacheHitReconstructsPrefixPlusDelta() {
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, 0);
    cache.put("a", events(5));
    OrchestratorService.WorkflowRequest request = OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId("a")
        .addAllPastEvents(events(3))
        .setCachedHistory(OrchestratorService.CachedHistory.newBuilder().setEventCount(5))
        .build();
    OrchestratorRunner runner = runner(request, cache);

    assertEquals(8, runner.resolvePastEvents("a").size()); // 5 cached prefix + 3 delta
  }

  @Test
  void disabledCacheReturnsRequestPastEvents() {
    OrchestratorService.WorkflowRequest request = OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId("a")
        .addAllPastEvents(events(4))
        .setCachedHistory(OrchestratorService.CachedHistory.newBuilder().setEventCount(2))
        .build();
    OrchestratorRunner runner = runner(request, null); // stateful history disabled

    assertEquals(4, runner.resolvePastEvents("a").size());
  }

  @Test
  void updateCachePutsWhenRunningThenEvictsOnComplete() {
    WorkflowHistoryCache cache = new WorkflowHistoryCache(null, 0, 0);
    OrchestratorService.WorkflowRequest request = OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId("a")
        .build();
    OrchestratorRunner runner = runner(request, cache);

    OrchestratorActions.WorkflowAction running = OrchestratorActions.WorkflowAction.newBuilder()
        .setScheduleTask(OrchestratorActions.ScheduleTaskAction.newBuilder())
        .build();
    runner.updateHistoryCache("a", events(6), resultWith(running));
    assertNotNull(cache.get("a"));

    OrchestratorActions.WorkflowAction completed = OrchestratorActions.WorkflowAction.newBuilder()
        .setCompleteWorkflow(OrchestratorActions.CompleteWorkflowAction.newBuilder())
        .build();
    runner.updateHistoryCache("a", events(6), resultWith(completed));
    assertNull(cache.get("a"));
  }

  @Test
  void updateCacheSkippedWhenDisabled() {
    OrchestratorService.WorkflowRequest request = OrchestratorService.WorkflowRequest.newBuilder()
        .setInstanceId("a")
        .build();
    OrchestratorRunner runner = runner(request, null); // no cache

    OrchestratorActions.WorkflowAction running = OrchestratorActions.WorkflowAction.newBuilder()
        .setScheduleTask(OrchestratorActions.ScheduleTaskAction.newBuilder())
        .build();
    // Must not throw when the cache is disabled (null).
    runner.updateHistoryCache("a", events(6), resultWith(running));
  }
}
