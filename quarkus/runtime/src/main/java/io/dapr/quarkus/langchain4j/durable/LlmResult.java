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
 * Output of the {@code agent-llm} activity.
 *
 * <p>Carries the <em>full</em> {@code AiMessage} serialized as LangChain4j JSON — including
 * any tool-execution requests (ids, names, arguments). This losslessness is what lets the
 * orchestrator rebuild the exact conversation on replay; a text-only summary would not.
 *
 * @param aiMessageJson the assistant message serialized as LangChain4j JSON
 */
public record LlmResult(String aiMessageJson) {
}
