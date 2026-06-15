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
 * Durable parallel composite: runs all sub-agents concurrently as child workflows from the same
 * seed state, then merges each result (leaf output under its key; a nested composite's whole state)
 * and applies the optional {@code @Output} combiner.
 *
 * <p>Control-inversion replacement for {@code ParallelOrchestrationWorkflow}; reuses
 * {@link DurableSequenceInput} (sub-agents are independent here — no inter-step threading).
 */
@ApplicationScoped
@WorkflowMetadata(name = "durable-parallel")
public class DurableParallelWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      DurableSequenceInput input = ctx.getInput(DurableSequenceInput.class);
      Map<String, String> state = new HashMap<>(input.initialState());

      DurableChildren.runParallel(ctx, input.subAgents(), state);

      String result = DurableOutput.resolve(input.combiner(), input.finalOutputKey(), state, null);
      if (input.finalOutputKey() != null && result != null) {
        state.put(input.finalOutputKey(), result);
      }
      ctx.complete(state);
    };
  }
}
