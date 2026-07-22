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

package io.dapr.quarkus.langchain4j.agent.recovery;

import java.util.List;

/**
 * Input for {@link RecoveryAgentActivity}. Contains everything needed to re-run
 * the agent's ReAct loop from scratch after a crash.
 *
 * @param agentRunId      the original agent run ID (for logging/correlation)
 * @param agentName       the agent name from {@code @Agent(name=...)}
 * @param systemMessage   the system message for the LLM
 * @param userMessage     the user message for the LLM
 * @param toolClassNames  fully-qualified class names of {@code @Tool} classes
 *                        (from {@code @ToolBox} annotation)
 */
public record RecoveryAgentInput(
    String agentRunId,
    String agentName,
    String systemMessage,
    String userMessage,
    List<String> toolClassNames) {
}
