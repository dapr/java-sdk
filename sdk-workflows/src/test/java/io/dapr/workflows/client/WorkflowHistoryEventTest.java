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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkflowHistoryEventTest {

  @Test
  public void exposesFields() {
    Instant now = Instant.ofEpochSecond(1000, 5);
    WorkflowHistoryEvent event = new WorkflowHistoryEvent(3, WorkflowHistoryEventType.TASK_SCHEDULED, now);
    assertEquals(3, event.getEventId());
    assertEquals(WorkflowHistoryEventType.TASK_SCHEDULED, event.getEventType());
    assertEquals(now, event.getTimestamp());
  }
}
