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
 * Input to {@link DurableConditionalWorkflow}.
 *
 * <p>Branches are evaluated in order; the first whose {@code @ActivationCondition} predicate
 * returns {@code true} (given the seed state) runs its sub-agent.
 *
 * @param branches       the candidate branches, in declaration order
 * @param initialState   seed state for condition evaluation and template rendering
 * @param finalOutputKey state key to return; {@code null} returns the chosen agent's output
 * @param combiner       optional {@code @Output} combiner producing the result; {@code null} if none
 */
public record DurableConditionalInput(
    List<ConditionalBranch> branches,
    Map<String, String> initialState,
    String finalOutputKey,
    OutputCombiner combiner) {
}
