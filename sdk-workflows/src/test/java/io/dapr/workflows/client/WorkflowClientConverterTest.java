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
    assertEquals(WorkflowHistoryEventType.EXECUTION_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.EXECUTIONSTARTED));
    assertEquals(WorkflowHistoryEventType.TASK_SCHEDULED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.TASKSCHEDULED));
    assertEquals(WorkflowHistoryEventType.WORKFLOW_STARTED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.WORKFLOWSTARTED));
    assertEquals(WorkflowHistoryEventType.CHILD_WORKFLOW_INSTANCE_CREATED,
        WorkflowClientConverter.toEventType(HistoryEvent.EventTypeCase.DETACHEDWORKFLOWINSTANCECREATED));
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
