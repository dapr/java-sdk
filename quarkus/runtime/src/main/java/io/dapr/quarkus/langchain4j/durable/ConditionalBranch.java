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

/**
 * One branch of a durable conditional composite: a sub-agent plus the {@code @ActivationCondition}
 * static method that guards it.
 *
 * <p>The condition is a pure static predicate (over {@code @V}-named scope values), so it can be
 * resolved at build time and invoked reflectively at run time on any replica — no opaque lambda
 * to serialize.
 *
 * @param agent          the sub-agent node to run if this branch is selected (leaf or composite)
 * @param conditionClass FQCN declaring the {@code @ActivationCondition} method ({@code null} = always)
 * @param conditionMethod the static predicate method name ({@code null} = always)
 * @param conditionVars  the condition method's {@code @V} parameter names, in order
 */
public record ConditionalBranch(
    AgentMethodMeta agent,
    String conditionClass,
    String conditionMethod,
    List<String> conditionVars) {
}
