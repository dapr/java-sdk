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

package io.dapr.workflows.client;

import io.dapr.durabletask.implementation.protobuf.Orchestration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowRuntimeStatusTest {

  /**
   * Before the durable task client was folded in, this enum was a copy of the task-side enum
   * that lacked STALLED, and a hand-written converter threw IllegalArgumentException on it.
   * A stalled workflow therefore made getWorkflowState() throw instead of reporting status.
   */
  @Test
  public void stalledIsMappedRatherThanRejected() {
    assertEquals(WorkflowRuntimeStatus.STALLED,
        WorkflowRuntimeStatus.fromProtobuf(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_STALLED));
    assertEquals(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_STALLED,
        WorkflowRuntimeStatus.toProtobuf(WorkflowRuntimeStatus.STALLED));
  }

  /**
   * The deleted WorkflowRuntimeStatusConverter had a test for its rejection branch. The mapping it
   * was replaced by has the same branch, and it is reachable: a sidecar newer than this SDK can
   * send a status this enum does not know, which protobuf surfaces as UNRECOGNIZED.
   */
  @Test
  public void anUnknownProtobufStatusIsRejectedRatherThanMisreported() {
    assertThrows(IllegalArgumentException.class,
        () -> WorkflowRuntimeStatus.fromProtobuf(
            Orchestration.OrchestrationStatus.UNRECOGNIZED));
  }

  @ParameterizedTest
  @EnumSource(WorkflowRuntimeStatus.class)
  public void everyStatusRoundTripsThroughProtobuf(WorkflowRuntimeStatus status) {
    assertEquals(status, WorkflowRuntimeStatus.fromProtobuf(WorkflowRuntimeStatus.toProtobuf(status)));
  }
}
