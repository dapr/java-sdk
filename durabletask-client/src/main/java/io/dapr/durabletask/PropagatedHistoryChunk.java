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

import com.google.protobuf.InvalidProtocolBufferException;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a chunk of propagated history events from a specific workflow instance.
 * Each chunk contains history events produced by the source workflow instance.
 * When created from protobuf data, the raw event bytes are parsed eagerly into
 * typed history events.
 */
public final class PropagatedHistoryChunk {
  private final String appId;
  private final String instanceId;
  private final String workflowName;
  private final List<HistoryEvents.HistoryEvent> events;

  PropagatedHistoryChunk(String appId,
                         String instanceId,
                         String workflowName,
                         List<HistoryEvents.HistoryEvent> events) {
    this.appId = appId;
    this.instanceId = instanceId;
    this.workflowName = workflowName;
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
  }

  static PropagatedHistoryChunk fromProto(HistoryEvents.PropagatedHistoryChunk proto) {
    List<HistoryEvents.HistoryEvent> parsed = new ArrayList<>(proto.getRawEventsCount());
    for (int i = 0; i < proto.getRawEventsCount(); i++) {
      try {
        parsed.add(HistoryEvents.HistoryEvent.parseFrom(proto.getRawEvents(i)));
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(
            "Failed to parse raw event " + i + " in chunk for app " + proto.getAppId(), e);
      }
    }
    return new PropagatedHistoryChunk(
        proto.getAppId(),
        proto.getInstanceId(),
        proto.getWorkflowName(),
        parsed);
  }

  /**
   * Gets the app ID that produced the events in this chunk.
   *
   * @return the app ID
   */
  public String getAppId() {
    return this.appId;
  }

  /**
   * Gets the workflow instance ID that produced the events in this chunk.
   *
   * @return the instance ID
   */
  public String getInstanceId() {
    return this.instanceId;
  }

  /**
   * Gets the workflow name that produced the events in this chunk.
   *
   * @return the workflow name
   */
  public String getWorkflowName() {
    return this.workflowName;
  }

  /**
   * Gets the history events that were produced in this chunk.
   *
   * @return an unmodifiable list of history events
   */
  public List<HistoryEvents.HistoryEvent> getEvents() {
    return this.events;
  }

  /**
   * Gets the number of events in this chunk.
   *
   * @return the event count
   */
  public int getEventCount() {
    return this.events.size();
  }
}
