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
limitations under the License.
*/

package io.dapr.workflows.task.client;

import io.dapr.workflows.client.WorkflowRuntimeStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PurgeInstanceCriteriaTest {

  @Test
  public void shouldStartWithNoCriteria() {
    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria();

    assertNull(criteria.getCreatedTimeFrom());
    assertNull(criteria.getCreatedTimeTo());
    assertNull(criteria.getTimeout());
    assertTrue(criteria.getRuntimeStatusList().isEmpty(),
        "an empty status list means every runtime status is selected");
  }

  @Test
  public void shouldReturnEachValueThatWasSet() {
    Instant from = Instant.parse("2025-01-01T00:00:00Z");
    Instant to = Instant.parse("2025-02-01T00:00:00Z");
    Duration timeout = Duration.ofSeconds(30);
    List<WorkflowRuntimeStatus> statuses =
        Arrays.asList(WorkflowRuntimeStatus.COMPLETED, WorkflowRuntimeStatus.FAILED);

    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
        .setCreatedTimeFrom(from)
        .setCreatedTimeTo(to)
        .setRuntimeStatusList(statuses)
        .setTimeout(timeout);

    assertEquals(from, criteria.getCreatedTimeFrom());
    assertEquals(to, criteria.getCreatedTimeTo());
    assertEquals(statuses, criteria.getRuntimeStatusList());
    assertEquals(timeout, criteria.getTimeout());
  }

  @Test
  public void shouldReturnItselfFromEverySetterSoCallsCanChain() {
    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria();

    assertSame(criteria, criteria.setCreatedTimeFrom(Instant.EPOCH));
    assertSame(criteria, criteria.setCreatedTimeTo(Instant.EPOCH));
    assertSame(criteria, criteria.setRuntimeStatusList(Arrays.asList(WorkflowRuntimeStatus.RUNNING)));
    assertSame(criteria, criteria.setTimeout(Duration.ZERO));
  }

  @Test
  public void shouldAcceptNullToClearACriteria() {
    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
        .setCreatedTimeFrom(Instant.EPOCH)
        .setCreatedTimeTo(Instant.EPOCH)
        .setTimeout(Duration.ofSeconds(1));

    criteria.setCreatedTimeFrom(null).setCreatedTimeTo(null).setTimeout(null);

    assertNull(criteria.getCreatedTimeFrom());
    assertNull(criteria.getCreatedTimeTo());
    assertNull(criteria.getTimeout(), "a null timeout resets it to the default");
  }
}
