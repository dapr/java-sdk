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
 * Input record for {@link LlmCallActivity}, identifying the specific LLM call to execute.
 *
 * @param agentRunId  the ID of the {@code AgentRunWorkflow} instance
 * @param llmCallId   the unique ID of the pending LLM call registered in
 *                    {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param methodName  name of the {@code ChatModel} method being called (e.g., {@code "chat"});
 *                    stored in the Dapr activity input for observability in the workflow history
 * @param prompt      string representation of the {@code ChatRequest} messages sent to the LLM;
 *                    extracted by {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}
 *                    and stored in the Dapr activity input so the full prompt is visible in the
 *                    workflow history without needing to inspect in-process state
 */
public record LlmCallInput(String agentRunId, String llmCallId, String methodName, String prompt) {
}
