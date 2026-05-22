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

import io.dapr.durabletask.implementation.protobuf.Orchestration;

/**
 * Controls how execution history is propagated to a child workflow or activity.
 */
public enum HistoryPropagationScope {
  /**
   * No propagation. The child receives no history from the caller.
   */
  NONE,

  /**
   * Propagate the caller's own history events only. The child does not see
   * any ancestral history (trust boundary).
   */
  OWN_HISTORY,

  /**
   * Propagate the caller's own history events AND the full ancestral chain.
   * Any propagated history this workflow received from its parent is forwarded to the child.
   */
  LINEAGE;

  Orchestration.HistoryPropagationScope toProto() {
    switch (this) {
      case OWN_HISTORY:
        return Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_OWN_HISTORY;
      case LINEAGE:
        return Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_LINEAGE;
      default:
        return Orchestration.HistoryPropagationScope.HISTORY_PROPAGATION_SCOPE_NONE;
    }
  }

  static HistoryPropagationScope fromProto(Orchestration.HistoryPropagationScope proto) {
    switch (proto) {
      case HISTORY_PROPAGATION_SCOPE_OWN_HISTORY:
        return OWN_HISTORY;
      case HISTORY_PROPAGATION_SCOPE_LINEAGE:
        return LINEAGE;
      default:
        return NONE;
    }
  }
}
