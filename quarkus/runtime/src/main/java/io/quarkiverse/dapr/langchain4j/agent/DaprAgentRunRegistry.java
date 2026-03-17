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

package io.quarkiverse.dapr.langchain4j.agent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry that maps agent run IDs to their {@link AgentRunContext}.
 *
 * <p>Similar to {@link io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry} but for
 * individual agent executions. Allows
 * {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity}
 * to look up the in-progress context for a given agent run ID.
 */
public class DaprAgentRunRegistry {

  private static final Map<String, AgentRunContext> REGISTRY = new ConcurrentHashMap<>();

  private DaprAgentRunRegistry() {
  }

  /**
   * Registers an agent run context for the given agent run ID.
   *
   * @param agentRunId the agent run ID
   * @param context    the agent run context to register
   */
  public static void register(String agentRunId, AgentRunContext context) {
    REGISTRY.put(agentRunId, context);
  }

  /**
   * Returns the agent run context for the given agent run ID.
   *
   * @param agentRunId the agent run ID
   * @return the agent run context, or {@code null} if not registered
   */
  public static AgentRunContext get(String agentRunId) {
    return REGISTRY.get(agentRunId);
  }

  /**
   * Unregisters the agent run context for the given agent run ID.
   *
   * @param agentRunId the agent run ID to unregister
   */
  public static void unregister(String agentRunId) {
    REGISTRY.remove(agentRunId);
  }

  /**
   * Returns the set of all registered agent run IDs.
   *
   * @return the set of registered agent run IDs
   */
  public static Set<String> getRegisteredIds() {
    return REGISTRY.keySet();
  }
}
