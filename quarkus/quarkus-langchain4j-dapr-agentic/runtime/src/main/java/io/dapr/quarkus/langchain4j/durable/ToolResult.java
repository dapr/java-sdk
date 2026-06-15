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
 * Output of the {@code agent-tool} activity.
 *
 * @param toolCallId the tool-call id this result corresponds to
 * @param toolName   the tool that produced the result
 * @param resultText the tool's result, rendered as text for the model
 */
public record ToolResult(String toolCallId, String toolName, String resultText) {
}
