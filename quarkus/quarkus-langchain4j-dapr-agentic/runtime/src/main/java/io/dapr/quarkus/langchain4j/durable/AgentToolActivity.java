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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.quarkus.langchain4j.agent.recovery.ToolRegistry;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Stateless activity that invokes one {@code @Tool} for {@link ReActAgentWorkflow}.
 *
 * <p>Resolves the tool by name via {@link ToolRegistry}, binds the model's JSON arguments to
 * the method parameters, and returns the result as text. Like {@link AgentLlmActivity}, it
 * depends only on its {@link ToolInput} and the (replica-wide) tool registry, obtained via
 * {@link Arc} since the workflow runtime instantiates activities by reflection.
 *
 * <p><b>At-least-once:</b> activities can be redelivered, so side-effecting tools must be
 * idempotent or externally guarded.
 */
@ApplicationScoped
@ActivityMetadata(name = "agent-tool")
public class AgentToolActivity implements WorkflowActivity {

  private static final Logger LOG = Logger.getLogger(AgentToolActivity.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public Object run(WorkflowActivityContext ctx) {
    ToolInput input = ctx.getInput(ToolInput.class);
    ToolRegistry toolRegistry = Arc.container().instance(ToolRegistry.class).get();
    Object[] args = parseArguments(toolRegistry, input.toolName(), input.arguments());
    String result = toolRegistry.invokeTool(input.toolName(), args);
    return new ToolResult(input.toolCallId(), input.toolName(), result);
  }

  private Object[] parseArguments(ToolRegistry toolRegistry, String toolName, String argsJson) {
    ToolRegistry.ToolEntry entry = toolRegistry.getToolEntry(toolName);
    if (entry == null) {
      return new Object[0];
    }
    Parameter[] params = entry.method().getParameters();
    if (params.length == 0) {
      return new Object[0];
    }
    try {
      Map<String, Object> argsMap = MAPPER.readValue(argsJson, new TypeReference<Map<String, Object>>() {
      });
      Object[] result = new Object[params.length];
      for (int i = 0; i < params.length; i++) {
        Object value = argsMap.get(params[i].getName());
        if (value == null && argsMap.size() == 1 && params.length == 1) {
          value = argsMap.values().iterator().next();
        }
        result[i] = MAPPER.convertValue(value, params[i].getType());
      }
      return result;
    } catch (Exception e) {
      LOG.warnf("Failed to parse args for tool '%s': %s — trying single-argument fallback",
          toolName, e.getMessage());
      return params.length == 1 ? new Object[]{argsJson} : new Object[params.length];
    }
  }
}
