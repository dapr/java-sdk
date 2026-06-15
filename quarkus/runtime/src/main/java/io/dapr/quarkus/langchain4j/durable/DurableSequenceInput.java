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

import java.util.List;
import java.util.Map;

/**
 * Input to {@link DurableSequenceWorkflow}.
 *
 * @param subAgents      the steps to run in order, each as a {@code react-agent} child
 * @param initialState   seed state (e.g. the request arguments) for template rendering
 * @param finalOutputKey state key to return as the composite result; {@code null} returns the
 *                       last step's output
 * @param combiner       optional {@code @Output} combiner producing the result; {@code null} if none
 */
public record DurableSequenceInput(
    List<AgentMethodMeta> subAgents,
    Map<String, String> initialState,
    String finalOutputKey,
    OutputCombiner combiner) {
}
