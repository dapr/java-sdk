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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.quarkus.arc.Arc;
import org.jboss.logging.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
  private static final ObjectMapper MAPPER = new ObjectMapper();

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

    Object input = DurableInputs.build(meta, state);
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

    return coerce(extractResult(meta, status), method.getReturnType());
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

  private static Object coerce(String raw, Class<?> returnType) throws Exception {
    if (returnType == void.class || returnType == Void.class) {
      return null;
    }
    if (returnType == String.class) {
      return raw;
    }
    if (raw == null) {
      return null;
    }
    // Structured return (e.g. a @Output record) serialized as JSON in the state map.
    return MAPPER.readValue(raw, returnType);
  }
}
