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
 * Input record for {@link AgentRunWorkflow}.
 *
 * @param agentRunId     unique ID correlating the Dapr Workflow instance to its in-memory
 *                       {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param agentName      human-readable name from {@code @Agent(name)} (or class+method for CDI
 *                       beans), used for observability in the Dapr workflow history
 * @param userMessage    the {@code @UserMessage} template text (CDI bean path) or the first
 *                       rendered user message from the {@code ChatRequest} (AiService path);
 *                       may be {@code null} when started by an orchestration activity
 * @param systemMessage  the {@code @SystemMessage} template text (CDI bean path) or the
 *                       rendered system message from the {@code ChatRequest} (AiService path);
 *                       may be {@code null}
 */
public record AgentRunInput(String agentRunId, String agentName, String userMessage, String systemMessage) {
}
