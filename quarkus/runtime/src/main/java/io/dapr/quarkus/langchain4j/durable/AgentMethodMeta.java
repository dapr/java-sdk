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
 * Build-time-extracted description of one agent method, used by
 * {@link DurableAgentInvocationHandler} to start the right durable workflow.
 *
 * <p>Recorder-serializable (record + Strings/ints/Lists), so it can be passed through a
 * {@code @Recorder} into the synthetic bean that replaces the AiServices-built agent.
 *
 * <p>This is a <b>recursive node</b>: {@code subAgents} (and {@code branches}) hold child
 * {@code AgentMethodMeta}s, so a composite can be another composite's sub-agent. A leaf node has
 * {@code workflowName == "react-agent"} and empty {@code subAgents}/{@code branches}.
 *
 * @param workflowName  target workflow: {@code react-agent} (leaf) or {@code durable-sequence}
 *                      / {@code durable-parallel} / {@code durable-loop} (composite)
 * @param agentName     agent name (leaf) or composite name; also used to name the run
 * @param userTemplate  leaf {@code @UserMessage} template, or {@code null}
 * @param systemTemplate leaf {@code @SystemMessage} template, or {@code null}
 * @param varNames      method parameter names in order (the {@code @V} names), for
 *                      {@code {{var}}} substitution / initial state
 * @param subAgents     child nodes (leaf or composite), empty for a leaf or conditional
 * @param outputKey     state key this node's result is stored under (in its parent's state)
 * @param maxIterations loop iteration count (0 if not a loop)
 * @param branches      conditional branches (empty unless a conditional composite)
 * @param combiner      optional {@code @Output} combiner for the composite result, or {@code null}
 */
public record AgentMethodMeta(
    String workflowName,
    String agentName,
    String userTemplate,
    String systemTemplate,
    List<String> varNames,
    List<AgentMethodMeta> subAgents,
    String outputKey,
    int maxIterations,
    List<ConditionalBranch> branches,
    OutputCombiner combiner) {
}
