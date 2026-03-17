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

package io.quarkiverse.dapr.examples;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI bean providing research tools for the {@link ResearchWriter} agent.
 *
 * <p>Because the {@code quarkus-agentic-dapr} extension automatically applies
 * {@code @DaprAgentToolInterceptorBinding} to all {@code @Tool}-annotated methods at
 * build time, every call to these methods that occurs inside a Dapr-backed agent run is
 * transparently routed through a {@code ToolCallActivity} Dapr Workflow Activity.
 *
 * <p>This means:
 * <ul>
 *   <li>Each tool call is recorded in the Dapr Workflow history.</li>
 *   <li>If the process crashes during a tool call, Dapr retries the activity automatically.</li>
 *   <li>No code changes are needed here — the routing is applied automatically.</li>
 * </ul>
 */
@ApplicationScoped
public class ResearchTools {

  /**
   * Looks up real-time population data for a given country.
   *
   * @param country the country to look up
   * @return population data string
   */
  @Tool("Looks up real-time population data for a given country")
  public String getPopulation(String country) {
    // In a real implementation this would call an external API.
    // Here we return a stub so the example runs without network access.
    return switch (country.toLowerCase()) {
      case "france" -> "France has approximately 68 million inhabitants (2024).";
      case "germany" -> "Germany has approximately 84 million inhabitants (2024).";
      case "japan" -> "Japan has approximately 124 million inhabitants (2024).";
      default -> country + " population data is not available in this demo.";
    };
  }

  /**
   * Returns the official capital city of a given country.
   *
   * @param country the country to look up
   * @return capital city string
   */
  @Tool("Returns the official capital city of a given country")
  public String getCapital(String country) {
    return switch (country.toLowerCase()) {
      case "france" -> "The capital of France is Paris.";
      case "germany" -> "The capital of Germany is Berlin.";
      case "japan" -> "The capital of Japan is Tokyo.";
      default -> "Capital city for " + country + " is not available in this demo.";
    };
  }
}
