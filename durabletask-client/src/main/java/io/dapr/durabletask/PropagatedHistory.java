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

import io.dapr.durabletask.implementation.protobuf.HistoryEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents propagated execution history from a parent workflow to a child
 * workflow or activity. Provides query methods for inspecting the
 * per-workflow contributions ({@link WorkflowResult}) and the raw event
 * stream.
 */
public final class PropagatedHistory {
  private final HistoryPropagationScope scope;
  private final List<WorkflowResult> workflows;

  PropagatedHistory(HistoryPropagationScope scope, List<WorkflowResult> workflows) {
    this.scope = scope;
    this.workflows = Collections.unmodifiableList(new ArrayList<>(workflows));
  }

  static PropagatedHistory fromProto(HistoryEvents.PropagatedHistory proto) {
    List<WorkflowResult> workflows = proto.getChunksList().stream()
        .map(WorkflowResult::fromProto)
        .collect(Collectors.toList());
    HistoryPropagationScope scope = HistoryPropagationScope.fromProto(proto.getScope());
    return new PropagatedHistory(scope, workflows);
  }

  /**
   * Gets the raw history events, flattened across all workflow contributions
   * in execution order.
   *
   * @return an unmodifiable list of history events
   */
  public List<HistoryEvents.HistoryEvent> getEvents() {
    List<HistoryEvents.HistoryEvent> all = new ArrayList<>();
    for (WorkflowResult wf : this.workflows) {
      all.addAll(wf.rawEvents());
    }
    return Collections.unmodifiableList(all);
  }

  /**
   * Gets the propagation scope that produced this history.
   *
   * @return the history propagation scope
   */
  public HistoryPropagationScope getScope() {
    return this.scope;
  }

  /**
   * Gets all workflow results in the propagated history chain, in execution
   * order (ancestor first, then own).
   *
   * @return an unmodifiable list of workflow results
   */
  public List<WorkflowResult> getWorkflows() {
    return this.workflows;
  }

  /**
   * Gets a deduplicated, ordered list of app IDs in the propagation chain.
   *
   * @return list of app IDs in the order they appear
   */
  public List<String> getAppIDs() {
    Set<String> seen = new LinkedHashSet<>();
    for (WorkflowResult wf : this.workflows) {
      if (wf.getAppId() != null && !wf.getAppId().isEmpty()) {
        seen.add(wf.getAppId());
      }
    }
    return new ArrayList<>(seen);
  }

  /**
   * Gets the most recent workflow in the propagated history whose name
   * matches. When the chain contains the same workflow name more than once
   * (e.g. ContinueAsNew or a recursive child workflow) this returns the
   * most-recent occurrence.
   *
   * @param name the workflow name to search for
   * @return the matching workflow result, or empty if none
   */
  public Optional<WorkflowResult> getLastWorkflowByName(String name) {
    List<WorkflowResult> matches = getWorkflowsByName(name);
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(matches.get(matches.size() - 1));
  }

  /**
   * Gets all workflow results matching the given name, in execution order.
   *
   * @param name the workflow name to search for
   * @return matching workflow results, possibly empty
   */
  public List<WorkflowResult> getWorkflowsByName(String name) {
    return this.workflows.stream()
        .filter(w -> name.equals(w.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Gets the history events produced by the given app ID.
   *
   * @param appId the app ID to filter by
   * @return events from the specified app
   */
  public List<HistoryEvents.HistoryEvent> getEventsByAppID(String appId) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (WorkflowResult wf : this.workflows) {
      if (appId.equals(wf.getAppId())) {
        result.addAll(wf.rawEvents());
      }
    }
    return result;
  }

  /**
   * Gets the history events produced by the given workflow instance ID.
   *
   * @param instanceId the instance ID to filter by
   * @return events from the specified instance
   */
  public List<HistoryEvents.HistoryEvent> getEventsByInstanceID(String instanceId) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (WorkflowResult wf : this.workflows) {
      if (instanceId.equals(wf.getInstanceId())) {
        result.addAll(wf.rawEvents());
      }
    }
    return result;
  }

  /**
   * Gets the history events produced by the given workflow name.
   *
   * @param workflowName the workflow name to filter by
   * @return events from the specified workflow
   */
  public List<HistoryEvents.HistoryEvent> getEventsByWorkflowName(String workflowName) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (WorkflowResult wf : this.workflows) {
      if (workflowName.equals(wf.getName())) {
        result.addAll(wf.rawEvents());
      }
    }
    return result;
  }
}
