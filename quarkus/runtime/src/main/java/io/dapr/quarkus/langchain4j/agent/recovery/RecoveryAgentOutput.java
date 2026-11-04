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

/**
 * Output from {@link RecoveryAgentActivity} after the agent's ReAct loop completes.
 *
 * @param agentName the agent name
 * @param result    the final text response from the LLM
 * @param llmCalls  number of LLM calls made during recovery
 * @param toolCalls number of tool calls made during recovery
 */
public record RecoveryAgentOutput(
    String agentName,
    String result,
    int llmCalls,
    int toolCalls) {
}
