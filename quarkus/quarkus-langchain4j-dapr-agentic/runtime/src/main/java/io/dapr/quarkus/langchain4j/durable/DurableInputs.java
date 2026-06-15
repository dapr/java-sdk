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

import java.util.Map;

/**
 * Builds the workflow input for an {@link AgentMethodMeta} node, given the current state.
 *
 * <p>Shared by the proxy entry point and the composite workflows so a node can be dispatched the
 * same way wherever it appears (top-level or as a nested sub-agent).
 */
final class DurableInputs {

  static final int MAX_STEPS = 16;

  private DurableInputs() {
  }

  /**
   * Builds the input object for {@code node.workflowName()}.
   *
   * @param node  the node to run
   * @param state the current state (rendered into leaf templates; seeds a composite child)
   * @return a {@link ReActInput} (leaf) or a {@code Durable*Input} (composite)
   */
  static Object build(AgentMethodMeta node, Map<String, String> state) {
    return switch (node.workflowName()) {
      case "react-agent" -> new ReActInput(
          node.agentName(),
          DurableRendering.render(node.systemTemplate(), state),
          DurableRendering.render(node.userTemplate(), state),
          null,
          null,
          MAX_STEPS);
      case "durable-sequence", "durable-parallel" -> new DurableSequenceInput(
          node.subAgents(), state, node.outputKey(), node.combiner());
      case "durable-loop" -> new DurableLoopInput(
          node.subAgents(), state, node.outputKey(), node.maxIterations(), node.combiner());
      case "durable-conditional" -> new DurableConditionalInput(
          node.branches(), state, node.outputKey(), node.combiner());
      default -> throw new IllegalStateException("Unsupported durable workflow: " + node.workflowName());
    };
  }
}
