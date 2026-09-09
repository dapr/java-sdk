/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.client;

import java.time.Instant;

/**
 * Represents a single event in a workflow instance's execution history.
 */
public final class WorkflowHistoryEvent {

  private final int eventId;
  private final WorkflowHistoryEventType eventType;
  private final Instant timestamp;

  /**
   * Constructs a workflow history event.
   *
   * @param eventId   the event ID within the workflow instance history
   * @param eventType the type of history event
   * @param timestamp the time the event occurred
   */
  public WorkflowHistoryEvent(int eventId, WorkflowHistoryEventType eventType, Instant timestamp) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.timestamp = timestamp;
  }

  /**
   * Gets the event ID within the workflow instance history.
   *
   * @return the event ID
   */
  public int getEventId() {
    return this.eventId;
  }

  /**
   * Gets the type of this history event.
   *
   * @return the event type
   */
  public WorkflowHistoryEventType getEventType() {
    return this.eventType;
  }

  /**
   * Gets the time this event occurred.
   *
   * @return the event timestamp
   */
  public Instant getTimestamp() {
    return this.timestamp;
  }
}
