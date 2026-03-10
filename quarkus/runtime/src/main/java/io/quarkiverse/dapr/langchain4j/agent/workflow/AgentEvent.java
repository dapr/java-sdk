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

/**
 * External event sent to {@link AgentRunWorkflow} via {@code DaprWorkflowClient.raiseEvent()}.
 *
 * <p>Two event types are used:
 * <ul>
 *   <li>{@code "tool-call"} — a {@code @Tool}-annotated method was intercepted; the workflow
 *       should schedule a {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity}.</li>
 *   <li>{@code "done"} — the agent has finished executing; the workflow should terminate.</li>
 * </ul>
 *
 * @param type       event discriminator: {@code "tool-call"} or {@code "done"}
 * @param toolCallId unique ID for this tool call (null for "done" events)
 * @param toolName   name of the tool method being called (null for "done" events)
 * @param args       serialized arguments (reserved for future use; null for now)
 */
public record AgentEvent(
    String type,
    String toolCallId,
    String toolName,
    String args) {
}
