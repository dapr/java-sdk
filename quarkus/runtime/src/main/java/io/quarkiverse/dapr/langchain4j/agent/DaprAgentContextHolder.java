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

/**
 * Thread-local holder for the current Dapr agent run ID.
 *
 * <p>Set by {@link io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner} before an agent
 * begins execution, so that {@link DaprToolCallInterceptor} can detect when a tool call
 * is happening inside a Dapr-backed agent and route it through a Dapr Workflow Activity.
 */
public class DaprAgentContextHolder {

  private static final ThreadLocal<String> AGENT_RUN_ID = new ThreadLocal<>();

  private DaprAgentContextHolder() {
  }

  /**
   * Sets the agent run ID for the current thread.
   *
   * @param agentRunId the agent run ID to set
   */
  public static void set(String agentRunId) {
    AGENT_RUN_ID.set(agentRunId);
  }

  /**
   * Returns the agent run ID for the current thread.
   *
   * @return the agent run ID, or {@code null} if not set
   */
  public static String get() {
    return AGENT_RUN_ID.get();
  }

  /**
   * Clears the agent run ID for the current thread.
   */
  public static void clear() {
    AGENT_RUN_ID.remove();
  }
}
