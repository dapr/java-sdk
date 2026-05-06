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

/**
 * Represents a chunk of propagated history events from a specific workflow instance.
 */
public final class PropagatedHistoryChunk {
  private final String appId;
  private final int startEventIndex;
  private final int eventCount;
  private final String instanceId;
  private final String workflowName;

  PropagatedHistoryChunk(String appId, int startEventIndex, int eventCount,
                         String instanceId, String workflowName) {
    this.appId = appId;
    this.startEventIndex = startEventIndex;
    this.eventCount = eventCount;
    this.instanceId = instanceId;
    this.workflowName = workflowName;
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
   * Gets the index of the first event in this chunk (inclusive) into the
   * propagated history events list.
   *
   * @return the start event index
   */
  public int getStartEventIndex() {
    return this.startEventIndex;
  }

  /**
   * Gets the number of events in this chunk.
   *
   * @return the event count
   */
  public int getEventCount() {
    return this.eventCount;
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
}
