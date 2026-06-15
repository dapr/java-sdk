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

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds the synthetic-bean instance for a durable agent: a {@code java.lang.reflect.Proxy}
 * implementing the agent interface, backed by {@link DurableAgentInvocationHandler}.
 *
 * <p>Mirrors {@code AgenticRecorder.createAiAgent} but replaces the AiServices-built agent with
 * one that runs as a Dapr Workflow. The deployment registers this as an alternative synthetic
 * bean (higher priority) so it wins over the quarkiverse-built bean.
 */
@Recorder
public class DurableAgentProxyRecorder {

  /**
   * Returns the creation function for the synthetic agent bean.
   *
   * @param ifaceName     the agent interface FQCN
   * @param methodMetas   per-method durable metadata, keyed by method name
   * @return a function that creates the proxy instance
   */
  public Function<SyntheticCreationalContext<Object>, Object> createAgentProxy(
      String ifaceName, Map<String, AgentMethodMeta> methodMetas) {
    return ctx -> {
      try {
        Class<?> iface = Class.forName(
            ifaceName, true, Thread.currentThread().getContextClassLoader());
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
            new DurableAgentInvocationHandler(methodMetas));
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Cannot load durable agent interface " + ifaceName, e);
      }
    };
  }
}
