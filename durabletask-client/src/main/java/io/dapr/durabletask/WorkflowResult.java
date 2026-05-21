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
import java.util.Optional;

/**
 * A scoped view of a single workflow's contribution to propagated history.
 * Carries the workflow's identity and exposes typed lookups
 * ({@link #getLastActivityByName(String)}, {@link #getLastChildWorkflowByName(String)},
 * and their plural counterparts) so callers can introspect what happened in
 * that workflow without dealing with raw history events.
 */
public final class WorkflowResult {
  private final String appId;
  private final String instanceId;
  private final String workflowName;
  private final List<HistoryEvents.HistoryEvent> events;

  WorkflowResult(String appId,
                 String instanceId,
                 String workflowName,
                 List<HistoryEvents.HistoryEvent> events) {
    this.appId = appId;
    this.instanceId = instanceId;
    this.workflowName = workflowName;
    this.events = Collections.unmodifiableList(new ArrayList<>(events));
  }

  static WorkflowResult fromProto(HistoryEvents.PropagatedHistoryChunk proto) {
    List<HistoryEvents.HistoryEvent> parsed = new ArrayList<>(proto.getRawEventsCount());
    for (int i = 0; i < proto.getRawEventsCount(); i++) {
      try {
        parsed.add(HistoryEvents.HistoryEvent.parseFrom(proto.getRawEvents(i)));
      } catch (InvalidProtocolBufferException e) {
        throw new PropagatedHistoryException(
            "Failed to parse raw event " + i + " for app " + proto.getAppId(), e);
      }
    }
    return new WorkflowResult(
        proto.getAppId(),
        proto.getInstanceId(),
        proto.getWorkflowName(),
        parsed);
  }

  /**
   * Gets the app ID that produced this workflow's events.
   *
   * @return the app ID
   */
  public String getAppId() {
    return this.appId;
  }

  /**
   * Gets the workflow instance ID.
   *
   * @return the instance ID
   */
  public String getInstanceId() {
    return this.instanceId;
  }

  /**
   * Gets the workflow name.
   *
   * @return the workflow name
   */
  public String getName() {
    return this.workflowName;
  }

  /**
   * Gets the most recent activity invocation in this workflow whose name
   * matches. Equivalent to the last element of
   * {@link #getActivitiesByName(String)}.
   *
   * @param name the activity name
   * @return the matching activity result, or empty if none
   */
  public Optional<ActivityResult> getLastActivityByName(String name) {
    List<ActivityResult> all = getActivitiesByName(name);
    if (all.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(all.get(all.size() - 1));
  }

  /**
   * Gets all activity invocations in this workflow matching the given name,
   * in execution order.
   *
   * @param name the activity name
   * @return matching activity results, possibly empty
   */
  public List<ActivityResult> getActivitiesByName(String name) {
    List<ActivityResult> results = new ArrayList<>();
    for (HistoryEvents.HistoryEvent e : this.events) {
      if (e.hasTaskScheduled() && name.equals(e.getTaskScheduled().getName())) {
        results.add(resolveActivity(e));
      }
    }
    return results;
  }

  /**
   * Gets the most recent child workflow invocation in this workflow whose
   * name matches. Equivalent to the last element of
   * {@link #getChildWorkflowsByName(String)}.
   *
   * @param name the child workflow name
   * @return the matching child workflow result, or empty if none
   */
  public Optional<ChildWorkflowResult> getLastChildWorkflowByName(String name) {
    List<ChildWorkflowResult> all = getChildWorkflowsByName(name);
    if (all.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(all.get(all.size() - 1));
  }

  /**
   * Gets all child workflow invocations in this workflow matching the given
   * name, in execution order.
   *
   * @param name the child workflow name
   * @return matching child workflow results, possibly empty
   */
  public List<ChildWorkflowResult> getChildWorkflowsByName(String name) {
    List<ChildWorkflowResult> results = new ArrayList<>();
    for (HistoryEvents.HistoryEvent e : this.events) {
      if (e.hasChildWorkflowInstanceCreated()
          && name.equals(e.getChildWorkflowInstanceCreated().getName())) {
        results.add(resolveChildWorkflow(e.getEventId(),
            e.getChildWorkflowInstanceCreated().getName()));
      }
    }
    return results;
  }

  // Matching is by the scheduling event's id (TaskCompletedEvent.taskScheduledId
  // / TaskFailedEvent.taskScheduledId), not by taskExecutionId — SDK retries
  // reuse the same taskExecutionId.
  private ActivityResult resolveActivity(HistoryEvents.HistoryEvent scheduleEvent) {
    HistoryEvents.TaskScheduledEvent ts = scheduleEvent.getTaskScheduled();
    int scheduleId = scheduleEvent.getEventId();
    boolean completed = false;
    boolean failed = false;
    com.google.protobuf.StringValue output = null;
    io.dapr.durabletask.implementation.protobuf.Orchestration.TaskFailureDetails error = null;
    for (HistoryEvents.HistoryEvent e : this.events) {
      if (e.hasTaskCompleted() && e.getTaskCompleted().getTaskScheduledId() == scheduleId) {
        completed = true;
        output = e.getTaskCompleted().getResult();
      }
      if (e.hasTaskFailed() && e.getTaskFailed().getTaskScheduledId() == scheduleId) {
        failed = true;
        error = e.getTaskFailed().getFailureDetails();
      }
    }
    return new ActivityResult(
        ts.getName(),
        completed,
        failed,
        ts.hasInput() ? ts.getInput() : null,
        output,
        error);
  }

  private ChildWorkflowResult resolveChildWorkflow(int eventId, String name) {
    boolean completed = false;
    boolean failed = false;
    com.google.protobuf.StringValue output = null;
    io.dapr.durabletask.implementation.protobuf.Orchestration.TaskFailureDetails error = null;
    for (HistoryEvents.HistoryEvent e : this.events) {
      if (e.hasChildWorkflowInstanceCompleted()
          && e.getChildWorkflowInstanceCompleted().getTaskScheduledId() == eventId) {
        completed = true;
        output = e.getChildWorkflowInstanceCompleted().getResult();
      }
      if (e.hasChildWorkflowInstanceFailed()
          && e.getChildWorkflowInstanceFailed().getTaskScheduledId() == eventId) {
        failed = true;
        error = e.getChildWorkflowInstanceFailed().getFailureDetails();
      }
    }
    return new ChildWorkflowResult(name, completed, failed, output, error);
  }

  // Package-private accessor used by PropagatedHistory for raw-event filtering.
  List<HistoryEvents.HistoryEvent> rawEvents() {
    return this.events;
  }
}
