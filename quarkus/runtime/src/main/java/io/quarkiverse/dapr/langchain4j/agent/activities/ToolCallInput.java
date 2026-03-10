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

package io.quarkiverse.dapr.langchain4j.agent.activities;

/**
 * Input record for {@link ToolCallActivity}.
 *
 * @param agentRunId  the agent run ID used to look up the
 *                    {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param toolCallId  the unique tool call ID used to look up the pending
 *                    {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext.PendingCall}
 * @param toolName    name of the {@code @Tool}-annotated method being executed; stored in the
 *                    Dapr activity input for observability in the workflow history
 * @param args        string representation of the arguments passed to the tool method
 */
public record ToolCallInput(String agentRunId, String toolCallId, String toolName, String args) {
}
