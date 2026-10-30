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

package io.dapr.quarkus.langchain4j.agent;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Binds agent names to their pending {@code agentRunId}s so the generated CDI decorator
 * can claim the right Dapr context on the agent's own executing thread.
 *
 * <p>In parallel orchestration, LangChain4j runs each agent on its own executor thread —
 * threads the planner does not own, so it cannot set {@link DaprAgentContextHolder} for
 * them. Instead, {@code AgentExecutionActivity} binds {@code agentName → agentRunId} at
 * submit time, and the generated decorator (which executes on the agent's thread and
 * knows its own agent name) claims the binding and sets the context before delegating.
 * This is what routes parallel agents' LLM and tool calls through their own
 * {@link io.dapr.quarkus.langchain4j.agent.workflow.AgentRunWorkflow}.
 *
 * <p>Bindings are claimed FIFO per agent name. If the same agent name is in flight for
 * multiple concurrent orchestrations, claims may cross-attribute runs between them —
 * run IDs are unique so routing still completes, but observability may interleave.
 */
public final class AgentRunBindingRegistry {

  private static final Map<String, Deque<String>> BINDINGS = new ConcurrentHashMap<>();

  private AgentRunBindingRegistry() {
  }

  /**
   * Binds a pending agent run to its agent name.
   *
   * @param agentName  the agent name (from {@code @Agent(name)} / {@code AgentInstance.name()})
   * @param agentRunId the agent run ID to claim later
   */
  public static void bind(String agentName, String agentRunId) {
    BINDINGS.computeIfAbsent(agentName, k -> new ConcurrentLinkedDeque<>()).add(agentRunId);
  }

  /**
   * Claims (removes and returns) the oldest pending run ID for the given agent name.
   *
   * @param agentName the agent name
   * @return the bound agentRunId, or {@code null} if none pending
   */
  public static String claim(String agentName) {
    Deque<String> queue = BINDINGS.get(agentName);
    return queue == null ? null : queue.poll();
  }

  /**
   * Removes a specific pending binding. Used by the planner when it routes a run on the
   * current thread itself (sequential execution): the binding will never be claimed by a
   * decorator, and leaving it behind would hand a dead run ID to a later claim of the
   * same agent name.
   *
   * @param agentName  the agent name
   * @param agentRunId the exact run ID to remove
   */
  public static void remove(String agentName, String agentRunId) {
    Deque<String> queue = BINDINGS.get(agentName);
    if (queue != null) {
      queue.remove(agentRunId);
    }
  }

  /**
   * Removes all pending bindings whose run ID starts with the given prefix. Safety net
   * used by the planner's cleanup so that an aborted orchestration cannot leak bindings
   * into later runs of the same agent names.
   *
   * @param agentName      the agent name
   * @param agentRunPrefix the run ID prefix (typically {@code plannerId + ":"})
   */
  public static void purge(String agentName, String agentRunPrefix) {
    Deque<String> queue = BINDINGS.get(agentName);
    if (queue != null) {
      queue.removeIf(id -> id.startsWith(agentRunPrefix));
    }
  }
}
