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

package io.dapr.quarkus.langchain4j.workflow;

/**
 * Shared utility methods for Dapr agent services.
 */
public final class DaprAgentServiceUtil {

  private DaprAgentServiceUtil() {
  }

  /**
   * Sanitizes a name for use as a Dapr workflow identifier.
   * Replaces any non-alphanumeric characters (except hyphens and underscores)
   * with underscores.
   *
   * @param name the name to sanitize
   * @return the sanitized name
   */
  public static String safeName(String name) {
    if (name == null || name.isEmpty()) {
      return "unnamed";
    }
    return name.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  /**
   * Converts an agent name to TitleCase for the workflow name convention.
   * E.g., "travel-planner-agent" becomes "TravelPlannerAgent".
   *
   * @param name the hyphen/underscore/space-separated name
   * @return the TitleCase version of the name
   */
  public static String toTitleCase(String name) {
    StringBuilder sb = new StringBuilder();
    boolean capitalizeNext = true;
    for (char c : name.toCharArray()) {
      if (c == '-' || c == '_' || c == ' ') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        sb.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Returns the workflow name for an agent following the convention:
   * {@code dapr.langchain4j.<TitleCaseName>.workflow}.
   * Falls back to {@code dapr.langchain4j.AgentRun.workflow} if name is
   * empty or "standalone".
   *
   * @param agentName the agent name (e.g., "weather-assistant")
   * @return the Dapr workflow name (e.g., "dapr.langchain4j.WeatherAssistant.workflow")
   */
  public static String agentWorkflowName(String agentName) {
    if (agentName == null || agentName.isEmpty() || "standalone".equals(agentName)) {
      return WorkflowNameResolver.resolve(
          io.dapr.quarkus.langchain4j.agent.workflow.AgentRunWorkflow.class);
    }
    return "dapr.langchain4j." + toTitleCase(agentName) + ".workflow";
  }

}
