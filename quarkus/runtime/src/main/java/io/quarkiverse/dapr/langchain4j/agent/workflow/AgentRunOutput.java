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

package io.quarkiverse.dapr.langchain4j.agent.workflow;

import io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallOutput;
import io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallOutput;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated output of a completed {@link AgentRunWorkflow}. Set as the Dapr
 * workflow custom status after every activity so observers can follow execution
 * progress in real time, and reflects the final state once {@code "done"} is received.
 *
 * @param agentName  human-readable name of the {@code @Agent} that was executed
 * @param toolCalls  ordered list of tool calls made by the agent, each with its
 *                   input arguments and return value
 * @param llmCalls   ordered list of LLM calls made by the agent, each with the
 *                   model method name and the response text
 */
public record AgentRunOutput(
    String agentName,
    List<ToolCallOutput> toolCalls,
    List<LlmCallOutput> llmCalls) {

  /**
   * Creates an AgentRunOutput with unmodifiable defensive copies of the lists.
   */
  public AgentRunOutput(String agentName, List<ToolCallOutput> toolCalls,
      List<LlmCallOutput> llmCalls) {
    this.agentName = agentName;
    this.toolCalls = toolCalls == null ? null : Collections.unmodifiableList(List.copyOf(toolCalls));
    this.llmCalls = llmCalls == null ? null : Collections.unmodifiableList(List.copyOf(llmCalls));
  }
}
