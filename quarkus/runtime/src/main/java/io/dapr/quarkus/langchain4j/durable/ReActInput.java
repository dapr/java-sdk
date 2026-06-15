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
 * Input to {@link ReActAgentWorkflow}.
 *
 * <p>The messages are already <em>rendered</em> (template variables substituted) at the
 * entry point where the call arguments are available — so the durable loop never deals
 * with raw {@code {{var}}} templates, and replay-based recovery cannot lose the binding.
 *
 * @param agentName         the agent name (used to resolve tool specifications)
 * @param systemMessage     the rendered system message, or {@code null}
 * @param userMessage       the rendered user message
 * @param priorMessagesJson serialized prior chat history (LangChain4j JSON), or {@code null}/blank
 * @param maxSteps          maximum ReAct iterations before giving up ({@code <= 0} uses the default)
 */
public record ReActInput(
    String agentName,
    String systemMessage,
    String userMessage,
    String priorMessagesJson,
    int maxSteps) {
}
