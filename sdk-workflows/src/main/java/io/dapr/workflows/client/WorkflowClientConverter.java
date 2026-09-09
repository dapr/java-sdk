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

import com.google.protobuf.Timestamp;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts durabletask proto messages to public workflow client model types.
 */
final class WorkflowClientConverter {

  private WorkflowClientConverter() {
  }

  static WorkflowInstancePage toWorkflowInstancePage(ListInstanceIDsResponse response) {
    return new WorkflowInstancePage(
        new ArrayList<>(response.getInstanceIdsList()),
        response.hasContinuationToken() ? response.getContinuationToken() : null);
  }

  static List<WorkflowHistoryEvent> toWorkflowHistory(List<HistoryEvent> events) {
    List<WorkflowHistoryEvent> result = new ArrayList<>(events.size());
    for (HistoryEvent event : events) {
      result.add(toWorkflowHistoryEvent(event));
    }
    return Collections.unmodifiableList(result);
  }

  static WorkflowHistoryEvent toWorkflowHistoryEvent(HistoryEvent event) {
    Instant timestamp = event.hasTimestamp() ? toInstant(event.getTimestamp()) : Instant.EPOCH;
    return new WorkflowHistoryEvent(event.getEventId(), toEventType(event.getEventTypeCase()), timestamp);
  }

  static WorkflowHistoryEventType toEventType(HistoryEvent.EventTypeCase eventType) {
    switch (eventType) {
      case EXECUTIONSTARTED:
        return WorkflowHistoryEventType.EXECUTION_STARTED;
      case EXECUTIONCOMPLETED:
        return WorkflowHistoryEventType.EXECUTION_COMPLETED;
      case EXECUTIONTERMINATED:
        return WorkflowHistoryEventType.EXECUTION_TERMINATED;
      case TASKSCHEDULED:
        return WorkflowHistoryEventType.TASK_SCHEDULED;
      case TASKCOMPLETED:
        return WorkflowHistoryEventType.TASK_COMPLETED;
      case TASKFAILED:
        return WorkflowHistoryEventType.TASK_FAILED;
      case CHILDWORKFLOWINSTANCECREATED:
      case DETACHEDWORKFLOWINSTANCECREATED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED;
      case CHILDWORKFLOWINSTANCECOMPLETED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_COMPLETED;
      case CHILDWORKFLOWINSTANCEFAILED:
        return WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_FAILED;
      case TIMERCREATED:
        return WorkflowHistoryEventType.TIMER_CREATED;
      case TIMERFIRED:
        return WorkflowHistoryEventType.TIMER_FIRED;
      case WORKFLOWSTARTED:
        return WorkflowHistoryEventType.WORKFLOW_STARTED;
      case WORKFLOWCOMPLETED:
        return WorkflowHistoryEventType.WORKFLOW_COMPLETED;
      case EVENTSENT:
        return WorkflowHistoryEventType.EVENT_SENT;
      case EVENTRAISED:
        return WorkflowHistoryEventType.EVENT_RAISED;
      case CONTINUEASNEW:
        return WorkflowHistoryEventType.CONTINUE_AS_NEW;
      case EXECUTIONSUSPENDED:
        return WorkflowHistoryEventType.EXECUTION_SUSPENDED;
      case EXECUTIONRESUMED:
        return WorkflowHistoryEventType.EXECUTION_RESUMED;
      case EXECUTIONSTALLED:
        return WorkflowHistoryEventType.EXECUTION_STALLED;
      default:
        return WorkflowHistoryEventType.UNKNOWN;
    }
  }

  private static Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }
}
