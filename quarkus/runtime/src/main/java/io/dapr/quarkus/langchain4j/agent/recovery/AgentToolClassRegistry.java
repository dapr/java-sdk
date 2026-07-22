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

package io.dapr.quarkus.langchain4j.agent.recovery;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping agent names to their {@code @ToolBox} class names.
 * Populated at build time by {@link io.dapr.quarkus.langchain4j.workflow.DaprWorkflowRuntimeRecorder}.
 */
public final class AgentToolClassRegistry {

  private static final Map<String, List<String>> REGISTRY = new ConcurrentHashMap<>();

  private AgentToolClassRegistry() {
  }

  /**
   * Registers the tool class names for a given agent.
   *
   * @param agentName      the agent name from {@code @Agent(name=...)}
   * @param toolClassNames fully-qualified class names from {@code @ToolBox}
   */
  public static void register(String agentName, List<String> toolClassNames) {
    REGISTRY.put(agentName, List.copyOf(toolClassNames));
  }

  /**
   * Returns the tool class names for the given agent, or an empty list if unknown.
   *
   * @param agentName the agent name
   * @return unmodifiable list of tool class FQCNs
   */
  public static List<String> get(String agentName) {
    return REGISTRY.getOrDefault(agentName, Collections.emptyList());
  }
}
