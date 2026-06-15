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
 * Input to the {@code agent-llm} activity: everything needed to make one model call,
 * with no dependency on in-memory state — so the activity can run on any replica.
 *
 * @param agentName    the agent name (used to resolve tool specifications)
 * @param messagesJson the full conversation so far, serialized as LangChain4j JSON
 */
public record LlmInput(String agentName, String messagesJson) {
}
