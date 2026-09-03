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
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents.HistoryEvent;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService.ListInstanceIDsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WorkflowClientConverterTest {

  @Test
  public void mapsEventTypeCases() {
    // Execution events
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONSTARTED));
    assertEquals(WorkflowHistoryEventType.EXECUTION_COMPLETED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONCOMPLETED));
    assertEquals(WorkflowHistoryEventType.EXECUTION_TERMINATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONTERMINATED));
    assertEquals(WorkflowHistoryEventType.EXECUTION_SUSPENDED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONSUSPENDED));
    assertEquals(WorkflowHistoryEventType.EXECUTION_RESUMED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONRESUMED));
    assertEquals(WorkflowHistoryEventType.EXECUTION_STALLED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONSTALLED));

    // Task events
    assertEquals(WorkflowHistoryEventType.TASK_SCHEDULED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TASKSCHEDULED));
    assertEquals(WorkflowHistoryEventType.TASK_COMPLETED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TASKCOMPLETED));
    assertEquals(WorkflowHistoryEventType.TASK_FAILED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TASKFAILED));

    // Child workflow events
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.CHILDWORKFLOWINSTANCECREATED));
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.DETACHEDWORKFLOWINSTANCECREATED));
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_COMPLETED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.CHILDWORKFLOWINSTANCECOMPLETED));
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_FAILED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.CHILDWORKFLOWINSTANCEFAILED));

    // Timer events
    assertEquals(WorkflowHistoryEventType.TIMER_CREATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TIMERCREATED));
    assertEquals(WorkflowHistoryEventType.TIMER_FIRED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TIMERFIRED));

    // Workflow events
    assertEquals(WorkflowHistoryEventType.WORKFLOW_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.WORKFLOWSTARTED));
    assertEquals(WorkflowHistoryEventType.WORKFLOW_COMPLETED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.WORKFLOWCOMPLETED));

    // Event communication
    assertEquals(WorkflowHistoryEventType.EVENT_SENT,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EVENTSENT));
    assertEquals(WorkflowHistoryEventType.EVENT_RAISED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EVENTRAISED));

    // Continue as new
    assertEquals(WorkflowHistoryEventType.CONTINUE_AS_NEW,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.CONTINUEASNEW));

    // Unknown/unset
    assertEquals(WorkflowHistoryEventType.UNKNOWN,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EVENTTYPE_NOT_SET));
  }

  @Test
  public void mapsHistoryEvent() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(7)
        .setTimestamp(Timestamp.newBuilder().setSeconds(1500).setNanos(500).build())
        .setExecutionStarted(HistoryEvents.ExecutionStartedEvent.getDefaultInstance())
        .build();

    WorkflowHistoryEvent result = WorkflowClientConverter.toWorkflowHistoryEvent(event);

    assertEquals(7, result.getEventId());
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED, result.getEventType());
    assertEquals(Instant.ofEpochSecond(1500, 500), result.getTimestamp());
  }

  @Test
  public void mapsHistoryList() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(1)
        .setTimerCreated(HistoryEvents.TimerCreatedEvent.getDefaultInstance())
        .build();

    assertEquals(1, WorkflowClientConverter.toWorkflowHistory(Arrays.asList(event)).size());
    assertEquals(WorkflowHistoryEventType.TIMER_CREATED,
        WorkflowClientConverter.toWorkflowHistory(Arrays.asList(event)).get(0).getEventType());
  }

  @Test
  public void usesEpochTimestampWhenNotSet() {
    HistoryEvent event = HistoryEvent.newBuilder()
        .setEventId(42)
        .setTaskScheduled(HistoryEvents.TaskScheduledEvent.getDefaultInstance())
        .build();

    WorkflowHistoryEvent result = WorkflowClientConverter.toWorkflowHistoryEvent(event);

    assertEquals(Instant.EPOCH, result.getTimestamp());
  }

  @Test
  public void mapsInstancePageWithToken() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder()
        .addInstanceIds("a").addInstanceIds("b")
        .setContinuationToken("next")
        .build();

    WorkflowInstancePage page = WorkflowClientConverter.toWorkflowInstancePage(response);

    assertEquals(Arrays.asList("a", "b"), page.getInstanceIds());
    assertEquals("next", page.getContinuationToken());
  }

  @Test
  public void mapsInstancePageWithoutToken() {
    ListInstanceIDsResponse response = ListInstanceIDsResponse.newBuilder().addInstanceIds("a").build();

    WorkflowInstancePage page = WorkflowClientConverter.toWorkflowInstancePage(response);

    assertNull(page.getContinuationToken());
  }
}
