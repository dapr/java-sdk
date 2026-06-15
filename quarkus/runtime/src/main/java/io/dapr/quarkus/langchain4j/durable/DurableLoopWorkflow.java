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
 * Durable loop composite: runs the sub-agent sequence a fixed number of iterations, threading
 * state across iterations (so each pass can refine the previous output).
 *
 * <p>Control-inversion replacement for {@code LoopOrchestrationWorkflow}. This representative
 * uses a counted loop; a predicate-based exit would evaluate a condition activity at the top
 * of each iteration (mirroring the existing {@code ExitConditionCheckActivity}).
 */
@ApplicationScoped
@WorkflowMetadata(name = "durable-loop")
public class DurableLoopWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      DurableLoopInput input = ctx.getInput(DurableLoopInput.class);
      Map<String, String> state = new HashMap<>(input.initialState());
      int iterations = input.maxIterations() > 0 ? input.maxIterations() : 1;

      for (int iteration = 0; iteration < iterations; iteration++) {
        DurableChildren.runSequential(ctx, input.subAgents(), state);
      }

      String result = DurableOutput.resolve(input.combiner(), input.finalOutputKey(), state, null);
      if (input.finalOutputKey() != null && result != null) {
        state.put(input.finalOutputKey(), result);
      }
      ctx.complete(state);
    };
  }
}
