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

package io.dapr.quarkus.langchain4j.durable;

import io.dapr.durabletask.Task;
import io.dapr.workflows.WorkflowContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs sub-agent child workflows and merges their results into the parent state — the durable
 * equivalent of LangChain4j's shared agentic scope.
 *
 * <p>A leaf child ({@code react-agent}) returns a {@code String} stored under its {@code outputKey};
 * a composite child ({@code durable-*}) returns its full state {@code Map}, which is merged into the
 * parent so inner keys (e.g. a nested parallel's outputs) propagate up to later sub-agents.
 */
final class DurableChildren {

  private DurableChildren() {
  }

  /** Runs children one at a time, merging each result before the next is built (rendered). */
  static void runSequential(WorkflowContext ctx, List<AgentMethodMeta> children, Map<String, String> state) {
    for (AgentMethodMeta child : children) {
      Object out = ctx.callChildWorkflow(
          child.workflowName(), DurableInputs.build(child, state), Object.class).await();
      merge(state, child, out);
    }
  }

  /** Runs children concurrently (all built from the same state), then merges all results. */
  static void runParallel(WorkflowContext ctx, List<AgentMethodMeta> children, Map<String, String> state) {
    if (children.isEmpty()) {
      return;
    }
    List<Task<Object>> tasks = new ArrayList<>();
    for (AgentMethodMeta child : children) {
      tasks.add(ctx.callChildWorkflow(
          child.workflowName(), DurableInputs.build(child, state), Object.class));
    }
    List<Object> results = ctx.allOf(tasks).await();
    for (int i = 0; i < children.size(); i++) {
      merge(state, children.get(i), results.get(i));
    }
  }

  private static void merge(Map<String, String> state, AgentMethodMeta child, Object out) {
    if (out instanceof Map<?, ?> childState) {
      // composite child: propagate its whole scope (inner keys included)
      for (Map.Entry<?, ?> entry : childState.entrySet()) {
        if (entry.getValue() != null) {
          state.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
      }
    } else if (out != null) {
      // leaf child: its text result, stored under its output key
      state.put(child.outputKey(), String.valueOf(out));
    }
  }
}
