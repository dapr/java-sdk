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

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.output.ServiceOutputParser;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.quarkus.arc.Arc;
import org.jboss.logging.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * The {@code java.lang.reflect.Proxy} handler behind a durable agent bean.
 *
 * <p>Replaces the AiServices-built agent: each call seeds state from the method arguments, starts
 * the matching durable workflow ({@code react-agent} for a leaf, {@code durable-*} for a composite),
 * waits for it, and returns the result. This is what makes the control-inversion engine "drop-in" —
 * the user's {@code @Agent} interface is unchanged.
 *
 * <p>A leaf workflow completes with its text ({@code String}); a composite completes with its full
 * state {@code Map}, from which the declared {@code outputKey} is read and coerced to the method's
 * return type (a structured record is deserialized from its JSON form).
 */
public class DurableAgentInvocationHandler implements InvocationHandler {

  private static final Logger LOG = Logger.getLogger(DurableAgentInvocationHandler.class);
  private static final int WAIT_MINUTES = 10;
  private static final ServiceOutputParser OUTPUT_PARSER = new ServiceOutputParser();

  private final Map<String, AgentMethodMeta> metasByMethod;

  public DurableAgentInvocationHandler(Map<String, AgentMethodMeta> metasByMethod) {
    this.metasByMethod = metasByMethod;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
    if (method.getDeclaringClass() == Object.class) {
      return switch (method.getName()) {
        case "toString" -> "DurableAgentProxy" + metasByMethod.keySet();
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == (args == null ? null : args[0]);
        default -> throw new UnsupportedOperationException(method.getName());
      };
    }

    AgentMethodMeta meta = metasByMethod.get(method.getName());
    if (meta == null) {
      throw new IllegalStateException("No durable metadata for agent method " + method.getName());
    }

    // Seed state from the @V parameter names.
    Map<String, String> state = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < meta.varNames().size() && i < args.length; i++) {
        if (args[i] != null) {
          state.put(meta.varNames().get(i), String.valueOf(args[i]));
        }
      }
    }

    Object input = withStructuredOutput(withMemory(DurableInputs.build(meta, state), method, args), method);
    String instanceId = meta.agentName() + "-" + UUID.randomUUID();
    LOG.infof("[DurableAgent:%s] starting %s workflow %s", meta.agentName(), meta.workflowName(), instanceId);

    DaprWorkflowClient client = Arc.container().instance(DaprWorkflowClient.class).get();
    client.scheduleNewWorkflow(meta.workflowName(), input, instanceId);
    WorkflowInstanceStatus status;
    try {
      status = client.waitForInstanceCompletion(instanceId, Duration.ofMinutes(WAIT_MINUTES), true);
    } catch (TimeoutException e) {
      throw new IllegalStateException(
          "Durable agent '" + meta.agentName() + "' did not complete within " + WAIT_MINUTES + "m", e);
    }

    return coerce(extractResult(meta, status), method);
  }

  /**
   * Pulls the raw result string: a leaf workflow's text, or a composite's {@code outputKey} value
   * from its final state map.
   */
  private static String extractResult(AgentMethodMeta meta, WorkflowInstanceStatus status) {
    if ("react-agent".equals(meta.workflowName())) {
      return status.readOutputAs(String.class);
    }
    Map<?, ?> finalState = status.readOutputAs(Map.class);
    if (finalState == null || meta.outputKey() == null) {
      return null;
    }
    Object value = finalState.get(meta.outputKey());
    return value == null ? null : String.valueOf(value);
  }

  /**
   * For a leaf agent with a structured (non-String) return type, append LangChain4j's
   * output-format instructions to the rendered user message so the model emits parseable JSON.
   * Composites produce their result via an {@code @Output} combiner — not directly from the model —
   * so they are left untouched.
   */
  private static Object withStructuredOutput(Object input, Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType == String.class || returnType == void.class || returnType == Void.class) {
      return input;
    }
    if (!(input instanceof ReActInput leaf)) {
      return input;
    }
    String instructions = OUTPUT_PARSER.outputFormatInstructions(method.getGenericReturnType());
    if (instructions == null || instructions.isBlank()) {
      return input;
    }
    String userMessage = (leaf.userMessage() == null ? "" : leaf.userMessage()) + "\n" + instructions;
    return new ReActInput(leaf.agentName(), leaf.systemMessage(), userMessage,
        leaf.priorMessagesJson(), leaf.memoryId(), leaf.maxSteps());
  }

  /**
   * For a leaf agent with a {@code @MemoryId} parameter, loads the prior conversation at the entry
   * (so it is captured into the workflow input and stays replay-stable) and tags the input with the
   * memory id so the workflow persists this turn at the end. Agents without {@code @MemoryId} are
   * stateless per call, exactly as AiServices is when no chat memory is configured.
   */
  private static Object withMemory(Object input, Method method, Object[] args) {
    if (!(input instanceof ReActInput leaf)) {
      return input;
    }
    int idx = memoryIdIndex(method);
    if (idx < 0 || args == null || idx >= args.length || args[idx] == null) {
      return input;
    }
    String memoryId = String.valueOf(args[idx]);
    String priorMessagesJson = DurableChatMemory.loadJson(memoryId);
    return new ReActInput(leaf.agentName(), leaf.systemMessage(), leaf.userMessage(),
        priorMessagesJson, memoryId, leaf.maxSteps());
  }

  private static int memoryIdIndex(Method method) {
    Parameter[] params = method.getParameters();
    for (int i = 0; i < params.length; i++) {
      if (params[i].isAnnotationPresent(MemoryId.class)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Coerces the workflow's raw result to the method's return type, reusing LangChain4j's
   * {@link ServiceOutputParser} (the same parser AiServices uses) so JSON objects, enums,
   * primitives and markdown-fenced output are all handled.
   */
  private static Object coerce(String raw, Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType == void.class || returnType == Void.class) {
      return null;
    }
    if (returnType == String.class) {
      return raw;
    }
    if (raw == null) {
      return null;
    }
    return OUTPUT_PARSER.parseText(method.getGenericReturnType(), raw);
  }
}
