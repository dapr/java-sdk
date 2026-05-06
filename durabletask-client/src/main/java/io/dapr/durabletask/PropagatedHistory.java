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

import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents propagated execution history from a parent workflow to a child workflow or activity.
 * Provides query methods for inspecting ancestor execution history.
 */
public final class PropagatedHistory {
  private final List<HistoryEvents.HistoryEvent> events;
  private final HistoryPropagationScope scope;
  private final List<PropagatedHistoryChunk> chunks;

  PropagatedHistory(List<HistoryEvents.HistoryEvent> events,
                    HistoryPropagationScope scope,
                    List<PropagatedHistoryChunk> chunks) {
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
    this.scope = scope;
    this.chunks = Collections.unmodifiableList(new ArrayList<>(chunks));
  }

  static PropagatedHistory fromProto(HistoryEvents.PropagatedHistory proto) {
    List<PropagatedHistoryChunk> chunks = proto.getChunksList().stream()
        .map(c -> new PropagatedHistoryChunk(
            c.getAppId(),
            c.getStartEventIndex(),
            c.getEventCount(),
            c.getInstanceId(),
            c.getWorkflowName()))
        .collect(Collectors.toList());

    HistoryPropagationScope scope = HistoryPropagationScope.fromProto(proto.getScope());

    return new PropagatedHistory(proto.getEventsList(), scope, chunks);
  }

  /**
   * Gets the raw history events that were propagated.
   *
   * @return an unmodifiable list of history events
   */
  public List<HistoryEvents.HistoryEvent> getEvents() {
    return this.events;
  }

  /**
   * Gets the propagation scope that was used to produce this history.
   *
   * @return the history propagation scope
   */
  public HistoryPropagationScope getScope() {
    return this.scope;
  }

  /**
   * Gets the workflow chunks identifying which app/workflow produced which events.
   *
   * @return an unmodifiable list of history chunks
   */
  public List<PropagatedHistoryChunk> getWorkflows() {
    return this.chunks;
  }

  /**
   * Gets a deduplicated, ordered list of app IDs in the propagation chain.
   *
   * @return list of app IDs in the order they appear in chunks
   */
  public List<String> getAppIDs() {
    Set<String> seen = new LinkedHashSet<>();
    for (PropagatedHistoryChunk chunk : this.chunks) {
      if (chunk.getAppId() != null && !chunk.getAppId().isEmpty()) {
        seen.add(chunk.getAppId());
      }
    }
    return new ArrayList<>(seen);
  }

  /**
   * Gets the first workflow chunk matching the given workflow name.
   * Returns the last match (most recent occurrence) if multiple exist.
   *
   * @param name the workflow name to search for
   * @return an Optional containing the matching chunk, or empty if not found
   */
  public Optional<PropagatedHistoryChunk> getWorkflowByName(String name) {
    List<PropagatedHistoryChunk> matches = getWorkflowsByName(name);
    if (matches.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(matches.get(matches.size() - 1));
  }

  /**
   * Gets all workflow chunks matching the given workflow name.
   *
   * @param name the workflow name to search for
   * @return a list of matching chunks in execution order
   */
  public List<PropagatedHistoryChunk> getWorkflowsByName(String name) {
    return this.chunks.stream()
        .filter(c -> name.equals(c.getWorkflowName()))
        .collect(Collectors.toList());
  }

  /**
   * Gets the history events produced by the given app ID.
   *
   * @param appId the app ID to filter by
   * @return a list of history events from the specified app
   */
  public List<HistoryEvents.HistoryEvent> getEventsByAppID(String appId) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (PropagatedHistoryChunk chunk : this.chunks) {
      if (appId.equals(chunk.getAppId())) {
        addEventsFromChunk(chunk, result);
      }
    }
    return result;
  }

  /**
   * Gets the history events produced by the given workflow instance ID.
   *
   * @param instanceId the instance ID to filter by
   * @return a list of history events from the specified instance
   */
  public List<HistoryEvents.HistoryEvent> getEventsByInstanceID(String instanceId) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (PropagatedHistoryChunk chunk : this.chunks) {
      if (instanceId.equals(chunk.getInstanceId())) {
        addEventsFromChunk(chunk, result);
      }
    }
    return result;
  }

  /**
   * Gets the history events produced by the given workflow name.
   *
   * @param workflowName the workflow name to filter by
   * @return a list of history events from the specified workflow
   */
  public List<HistoryEvents.HistoryEvent> getEventsByWorkflowName(String workflowName) {
    List<HistoryEvents.HistoryEvent> result = new ArrayList<>();
    for (PropagatedHistoryChunk chunk : this.chunks) {
      if (workflowName.equals(chunk.getWorkflowName())) {
        addEventsFromChunk(chunk, result);
      }
    }
    return result;
  }

  private void addEventsFromChunk(PropagatedHistoryChunk chunk,
                                  List<HistoryEvents.HistoryEvent> result) {
    int start = chunk.getStartEventIndex();
    int end = Math.min(start + chunk.getEventCount(), this.events.size());
    for (int i = start; i < end; i++) {
      result.add(this.events.get(i));
    }
  }
}
