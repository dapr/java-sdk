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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Resolves a composite's final result: invokes the static {@code @Output} combiner (if present)
 * with sub-agent outputs read from the accumulated state, otherwise returns the {@code outputKey}
 * value (or {@code null}).
 *
 * <p>{@code @Output} methods are pure combiners (format/aggregate scope values), so invoking them
 * reflectively in the orchestrator is deterministic and replay-safe.
 */
final class DurableOutput {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DurableOutput() {
  }

  /**
   * Computes the composite result as a String (a structured {@code @Output} record is serialized to
   * JSON so it can live in the state map; the proxy deserializes it back at the boundary).
   *
   * @param combiner   the {@code @Output} reference, or {@code null}
   * @param outputKey  the composite output key (used when there is no combiner)
   * @param state      the accumulated sub-agent outputs (by scope key)
   * @param fallback   value to return when neither combiner nor outputKey yields a result
   * @return the composite result as a String, or {@code null}
   */
  static String resolve(OutputCombiner combiner, String outputKey,
      Map<String, String> state, String fallback) {
    if (combiner != null) {
      return asString(invoke(combiner, state));
    }
    if (outputKey != null && state.get(outputKey) != null) {
      return state.get(outputKey);
    }
    return fallback;
  }

  private static String asString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return String.valueOf(value);
    }
  }

  private static Object invoke(OutputCombiner combiner, Map<String, String> state) {
    try {
      Class<?> declaring = Class.forName(
          combiner.declaringClass(), true, Thread.currentThread().getContextClassLoader());
      Method method = findMethod(declaring, combiner.methodName());
      Class<?>[] types = method.getParameterTypes();
      Object[] args = new Object[types.length];
      for (int i = 0; i < types.length; i++) {
        String value = i < combiner.paramNames().size() ? state.get(combiner.paramNames().get(i)) : null;
        args[i] = coerce(value, types[i]);
      }
      return method.invoke(null, args);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to invoke @Output combiner "
          + combiner.declaringClass() + "#" + combiner.methodName(), e);
    }
  }

  private static Method findMethod(Class<?> declaring, String name) throws NoSuchMethodException {
    for (Method m : declaring.getDeclaredMethods()) {
      if (m.getName().equals(name)) {
        m.setAccessible(true);
        return m;
      }
    }
    throw new NoSuchMethodException(declaring.getName() + "#" + name);
  }

  private static Object coerce(String value, Class<?> type) {
    if (type == String.class || value == null) {
      return value;
    }
    if (type == int.class || type == Integer.class) {
      return Integer.parseInt(value);
    }
    if (type == long.class || type == Long.class) {
      return Long.parseLong(value);
    }
    if (type == boolean.class || type == Boolean.class) {
      return Boolean.parseBoolean(value);
    }
    if (type == double.class || type == Double.class) {
      return Double.parseDouble(value);
    }
    return value;
  }
}
