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

import dev.langchain4j.agent.tool.ToolSpecification;
import io.dapr.quarkus.langchain4j.agent.recovery.AgentToolClassRegistry;
import io.dapr.quarkus.langchain4j.agent.recovery.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Resolves the {@link ToolSpecification}s advertised to the model for a given agent.
 *
 * <p>Thin seam over the existing build-time wiring: {@link AgentToolClassRegistry} maps an
 * agent name to its {@code @ToolBox} class names, and {@link ToolRegistry} turns those into
 * tool specifications. Because both are available on every replica, the {@code agent-llm}
 * activity can resolve tools wherever Dapr places it — no in-memory run state required.
 */
@ApplicationScoped
public class AgentToolSpecRegistry {

  @Inject
  ToolRegistry toolRegistry;

  /**
   * Returns the tool specifications for the given agent, or an empty list if it has none.
   *
   * @param agentName the agent name
   * @return the tool specifications (never {@code null})
   */
  public List<ToolSpecification> specsFor(String agentName) {
    List<String> toolClassNames = AgentToolClassRegistry.get(agentName);
    if (toolClassNames == null || toolClassNames.isEmpty()) {
      return List.of();
    }
    return toolRegistry.getToolSpecsForClasses(toolClassNames);
  }
}
