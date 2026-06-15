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

/**
 * Input to the {@code agent-tool} activity: one tool call requested by the model.
 *
 * @param agentName  the agent name (used to resolve the tool)
 * @param toolCallId the LLM-assigned tool-call id (echoed back to correlate the result)
 * @param toolName   the tool name to invoke
 * @param arguments  the tool arguments as the JSON string produced by the model
 */
public record ToolInput(String agentName, String toolCallId, String toolName, String arguments) {
}
