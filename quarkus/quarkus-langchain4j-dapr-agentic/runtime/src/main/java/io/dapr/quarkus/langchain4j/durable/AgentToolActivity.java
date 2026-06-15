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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import io.dapr.quarkus.langchain4j.agent.recovery.ToolRegistry;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Stateless activity that invokes one {@code @Tool} for {@link ReActAgentWorkflow}.
 *
 * <p>Resolves the tool by name via {@link ToolRegistry} and executes it with LangChain4j's
 * {@link DefaultToolExecutor} — the same binder AiServices uses — so JSON arguments are bound
 * to the method signature with full fidelity ({@code @P} parameters, nested/complex types).
 * Like {@link AgentLlmActivity}, it depends only on its {@link ToolInput} and the (replica-wide)
 * tool registry, obtained via {@link Arc} since the workflow runtime instantiates activities by
 * reflection.
 *
 * <p><b>Self-correction:</b> an unknown (hallucinated) tool, or a binding/execution failure,
 * returns the error text as the tool result rather than failing the workflow, so the model can
 * react and retry on the next turn (LangChain4j ReAct semantics).
 *
 * <p><b>At-least-once:</b> activities can be redelivered, so side-effecting tools must be
 * idempotent or externally guarded.
 */
@ApplicationScoped
@ActivityMetadata(name = "agent-tool")
public class AgentToolActivity implements WorkflowActivity {

  private static final Logger LOG = Logger.getLogger(AgentToolActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    ToolInput input = ctx.getInput(ToolInput.class);
    ToolRegistry toolRegistry = Arc.container().instance(ToolRegistry.class).get();
    ToolRegistry.ToolEntry entry = toolRegistry.getToolEntry(input.toolName());

    // Unknown / hallucinated tool: feed an error back to the model instead of crashing the
    // workflow, so the loop can self-correct.
    if (entry == null) {
      LOG.warnf("Model requested unknown tool '%s'", input.toolName());
      return new ToolResult(input.toolCallId(), input.toolName(),
          "Error: there is no tool named '" + input.toolName() + "'.");
    }

    Object beanInstance = Arc.container().instance(entry.beanClass()).get();
    ToolExecutionRequest request = ToolExecutionRequest.builder()
        .id(input.toolCallId())
        .name(input.toolName())
        .arguments(input.arguments())
        .build();

    String result;
    try {
      // DefaultToolExecutor parses the JSON arguments against the method signature and invokes
      // the tool; on failure it surfaces the error so we can return it to the model.
      result = new DefaultToolExecutor(beanInstance, entry.method()).execute(request, null);
    } catch (Exception e) {
      LOG.warnf("Tool '%s' failed: %s", input.toolName(), e.getMessage());
      result = "Error: " + e.getMessage();
    }
    return new ToolResult(input.toolCallId(), input.toolName(), result);
  }
}
