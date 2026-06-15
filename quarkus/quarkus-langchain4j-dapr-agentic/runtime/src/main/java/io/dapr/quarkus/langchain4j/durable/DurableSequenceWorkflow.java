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

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

/**
 * Durable sequential composite: runs each sub-agent in order as a {@code react-agent} child
 * workflow, threading shared state between steps.
 *
 * <p>Children are called directly via {@link io.dapr.workflows.WorkflowContext#callChildWorkflow}.
 * Because each child is itself a durable workflow (a leaf {@link ReActAgentWorkflow} or a nested
 * composite), the whole tree is replayable and replica-agnostic — all agent state lives in
 * workflow history, with no in-memory planner, queue, or per-thread context.
 *
 * <p>The parallel / loop / conditional composites follow the same shape ({@code allOf} for
 * parallel, a counted/condition loop, an {@code if} on a condition).
 */
@ApplicationScoped
@WorkflowMetadata(name = "durable-sequence")
public class DurableSequenceWorkflow implements Workflow {

  private static final int CHILD_MAX_STEPS = 16;

  @Override
  public WorkflowStub create() {
    return ctx -> {
      DurableSequenceInput input = ctx.getInput(DurableSequenceInput.class);
      Map<String, String> state = new HashMap<>(input.initialState());

      // Each sub-agent (leaf or nested composite) runs as a child workflow; its outputs merge into
      // state before the next renders.
      DurableChildren.runSequential(ctx, input.subAgents(), state);

      String result = DurableOutput.resolve(input.combiner(), input.finalOutputKey(), state, null);
      if (input.finalOutputKey() != null && result != null) {
        state.put(input.finalOutputKey(), result);
      }
      // Complete with the full state so a parent composite can propagate inner keys upward.
      ctx.complete(state);
    };
  }
}
