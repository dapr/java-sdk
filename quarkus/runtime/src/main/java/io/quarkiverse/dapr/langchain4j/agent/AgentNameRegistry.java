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

package io.quarkiverse.dapr.langchain4j.agent;

import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping {@code @Agent} interface names to agent names.
 *
 * <p>Populated at startup by
 * {@link io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowRuntimeRecorder}
 * and used to resolve the agent name when the generated CDI decorator
 * cannot fire (e.g., quarkus-langchain4j synthetic beans).
 */
public final class AgentNameRegistry {

  private static final Logger LOG = Logger.getLogger(AgentNameRegistry.class);
  private static final ConcurrentHashMap<String, String> REGISTRY = new ConcurrentHashMap<>();

  private AgentNameRegistry() {
  }

  /**
   * Registers a mapping from interface name to agent name.
   *
   * @param interfaceName the fully-qualified interface name
   * @param agentName     the agent name (e.g., "weather-assistant")
   */
  public static void register(String interfaceName, String agentName) {
    REGISTRY.put(interfaceName, agentName);
  }

  /**
   * Returns the agent name for the given interface, or {@code null} if not registered.
   *
   * @param interfaceName the fully-qualified interface name
   * @return the agent name, or {@code null}
   */
  public static String get(String interfaceName) {
    return REGISTRY.get(interfaceName);
  }

  /**
   * Walks the call stack to find an AiService proxy that implements
   * a registered {@code @Agent} interface.
   *
   * @return the agent name, or {@code null} if not found
   */
  public static String resolveFromStack() {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(frames -> {
          var it = frames.iterator();
          while (it.hasNext()) {
            Class<?> clazz = it.next().getDeclaringClass();
            for (Class<?> iface : clazz.getInterfaces()) {
              String name = REGISTRY.get(iface.getName());
              if (name != null) {
                LOG.infof("Resolved agent '%s' from stack: %s -> %s",
                    name, clazz.getSimpleName(), iface.getSimpleName());
                return name;
              }
            }
          }
          return null;
        });
  }
}
