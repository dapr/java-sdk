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

package io.dapr.workflows.task.history;

import io.dapr.durabletask.implementation.protobuf.Orchestration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lives in this package because fromProto is package-private: it is called only by
 * PropagatedHistory, so it stays hidden rather than being widened for a test.
 */
public class HistoryPropagationScopeTest {

  @Test
  void fromProtoConvertsCorrectly() {
    assertEquals(HistoryPropagationScope.NONE,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_NONE));
    assertEquals(HistoryPropagationScope.OWN_HISTORY,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY));
    assertEquals(HistoryPropagationScope.LINEAGE,
        HistoryPropagationScope.fromProto(
            Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE));
  }
}
