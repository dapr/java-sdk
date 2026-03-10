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
 * Output record returned by {@link LlmCallActivity} after a {@code ChatModel.chat()}
 * call has been executed. Stored in the Dapr workflow history so the full LLM turn
 * (prompt in, response out) is visible without inspecting in-process state.
 *
 * @param methodName  name of the {@code ChatModel} method that was invoked (e.g., {@code "chat"})
 * @param prompt      serialized {@code ChatRequest} messages that were sent to the model;
 *                    extracted from the {@code ChatRequest} argument by
 *                    {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}
 * @param response    AI response text extracted from {@code ChatResponse.aiMessage().text()};
 *                    this is the exact text the model returned to the agent
 */
public record LlmCallOutput(String methodName, String prompt, String response) {
}
