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

package io.dapr.quarkus.langchain4j.agent.recovery;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import io.quarkus.arc.Arc;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDI bean that discovers all {@code @Tool}-annotated methods at startup and provides
 * tool specification lookup and invocation for crash recovery.
 *
 * <p>During recovery, the LangChain4j AiServices ReAct loop is gone, so tools must be
 * resolved and invoked independently. This registry provides that capability.
 */
@ApplicationScoped
public class ToolRegistry {

  private static final Logger LOG = Logger.getLogger(ToolRegistry.class);

  /**
   * Entry for a discovered tool: the CDI bean class, method handle, and LangChain4j spec.
   */
  public record ToolEntry(Class<?> beanClass, Method method, ToolSpecification specification) {
  }

  private final Map<String, ToolEntry> toolsByName = new HashMap<>();
  private final Map<String, List<ToolEntry>> toolsByClass = new HashMap<>();

  @PostConstruct
  void init() {
    var beanManager = Arc.container().beanManager();
    for (var bean : beanManager.getBeans(Object.class, jakarta.enterprise.inject.Any.Literal.INSTANCE)) {
      Class<?> beanClass = bean.getBeanClass();
      boolean hasTools = false;
      // getDeclaredMethods (not getMethods): matches LangChain4j's own
      // ToolSpecifications.toolSpecificationsFrom(), which only generates specs
      // for methods declared directly on the class — any access level.
      for (Method m : beanClass.getDeclaredMethods()) {
        if (m.isAnnotationPresent(Tool.class)) {
          hasTools = true;
          break;
        }
      }
      if (!hasTools) {
        continue;
      }

      List<ToolSpecification> specs;
      try {
        specs = ToolSpecifications.toolSpecificationsFrom(beanClass);
      } catch (Exception e) {
        LOG.debugf("Could not extract tool specs from %s: %s", beanClass.getName(), e.getMessage());
        continue;
      }

      // Match specs to methods by name
      List<ToolEntry> classEntries = new ArrayList<>();
      for (ToolSpecification spec : specs) {
        Method toolMethod = findToolMethod(beanClass, spec.name());
        if (toolMethod != null) {
          ToolEntry entry = new ToolEntry(beanClass, toolMethod, spec);
          toolsByName.put(spec.name(), entry);
          classEntries.add(entry);
        }
      }
      if (!classEntries.isEmpty()) {
        toolsByClass.put(beanClass.getName(), classEntries);
        LOG.infof("ToolRegistry: registered %d tools from %s", classEntries.size(), beanClass.getSimpleName());
      }
    }
    LOG.infof("ToolRegistry: %d tools registered total", toolsByName.size());
  }

  /**
   * Returns tool specifications for the given tool class names.
   *
   * @param classNames fully-qualified class names (from {@code @ToolBox})
   * @return list of tool specifications for those classes
   */
  public List<ToolSpecification> getToolSpecsForClasses(List<String> classNames) {
    List<ToolSpecification> result = new ArrayList<>();
    for (String className : classNames) {
      List<ToolEntry> entries = toolsByClass.get(className);
      if (entries != null) {
        for (ToolEntry entry : entries) {
          result.add(entry.specification());
        }
      }
    }
    return result;
  }

  /**
   * Returns all registered tool specifications.
   *
   * @return unmodifiable list of all tool specifications
   */
  public List<ToolSpecification> getAllToolSpecs() {
    List<ToolSpecification> result = new ArrayList<>();
    for (ToolEntry entry : toolsByName.values()) {
      result.add(entry.specification());
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Invokes a tool by name with the given arguments.
   *
   * @param toolName the tool name (as declared in {@code @Tool})
   * @param args     the arguments to pass (already resolved to match method parameters)
   * @return the tool result as a string
   */
  public String invokeTool(String toolName, Object[] args) {
    ToolEntry entry = toolsByName.get(toolName);
    if (entry == null) {
      throw new IllegalArgumentException("Unknown tool: " + toolName);
    }

    Object beanInstance = Arc.container().instance(entry.beanClass()).get();
    if (beanInstance == null) {
      throw new IllegalStateException("No CDI bean instance for tool class: " + entry.beanClass().getName());
    }

    try {
      Object result = entry.method().invoke(beanInstance, args);
      return String.valueOf(result);
    } catch (Exception e) {
      throw new RuntimeException("Tool invocation failed: " + toolName, e);
    }
  }

  /**
   * Returns the ToolEntry for the given tool name, or null if not found.
   *
   * @param toolName the tool name to look up
   * @return the ToolEntry, or null
   */
  public ToolEntry getToolEntry(String toolName) {
    return toolsByName.get(toolName);
  }

  private Method findToolMethod(Class<?> clazz, String toolName) {
    for (Method m : clazz.getDeclaredMethods()) {
      Tool toolAnn = m.getAnnotation(Tool.class);
      if (toolAnn != null) {
        // @Tool name defaults to method name if not specified
        String name = toolAnn.name().isEmpty() ? m.getName() : toolAnn.name();
        if (name.equals(toolName)) {
          m.setAccessible(true);
          return m;
        }
      }
    }
    return null;
  }
}
