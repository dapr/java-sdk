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

package io.quarkiverse.dapr.langchain4j.workflow;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.quarkiverse.dapr.langchain4j.agent.AgentNameRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

/**
 * Quarkus recorder that registers Dapr workflows and activities at runtime
 * using the Dapr Java SDK's {@link WorkflowRuntimeBuilder} directly.
 *
 * <p>This replaces quarkus-dapr's build-time {@code WorkflowItemBuildItem} pipeline,
 * giving full control over workflow naming (same class registered under multiple
 * names) and avoiding the deduplication-by-class limitation.
 */
@Recorder
public class DaprWorkflowRuntimeRecorder {

  private static final Logger LOG = Logger.getLogger(DaprWorkflowRuntimeRecorder.class);

  /**
   * Creates a new {@link WorkflowRuntimeBuilder}.
   *
   * @return a runtime value wrapping the builder
   */
  public RuntimeValue<WorkflowRuntimeBuilder> createBuilder() {
    return new RuntimeValue<>(new WorkflowRuntimeBuilder());
  }

  /**
   * Registers a workflow class under a custom name.
   *
   * @param builder   the builder runtime value
   * @param name      the workflow registration name
   * @param className the fully-qualified workflow class name
   */
  @SuppressWarnings("unchecked")
  public void registerWorkflow(RuntimeValue<WorkflowRuntimeBuilder> builder,
      String name, String className) {
    try {
      Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
      builder.getValue().registerWorkflow(name, (Class<? extends Workflow>) clazz);
      LOG.infof("Registered workflow: %s -> %s", name, clazz.getSimpleName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Workflow class not found: " + className, e);
    }
  }

  /**
   * Registers an activity class under a custom name.
   *
   * @param builder   the builder runtime value
   * @param name      the activity registration name
   * @param className the fully-qualified activity class name
   */
  @SuppressWarnings("unchecked")
  public void registerActivity(RuntimeValue<WorkflowRuntimeBuilder> builder,
      String name, String className) {
    try {
      Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
      builder.getValue().registerActivity(name, (Class<? extends WorkflowActivity>) clazz);
      LOG.infof("Registered activity: %s -> %s", name, clazz.getSimpleName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Activity class not found: " + className, e);
    }
  }

  /**
   * Registers a mapping from an {@code @Agent} interface name to its agent name.
   *
   * @param interfaceName the fully-qualified interface name
   * @param agentName     the agent name from {@code @Agent(name=...)}
   */
  public void registerAgentName(String interfaceName, String agentName) {
    AgentNameRegistry.register(interfaceName, agentName);
    LOG.infof("Registered agent name: %s -> %s", interfaceName, agentName);
  }

  /**
   * Builds the workflow runtime and starts it (non-blocking).
   *
   * @param builder the builder runtime value
   */
  public void startRuntime(RuntimeValue<WorkflowRuntimeBuilder> builder) {
    WorkflowRuntime runtime = builder.getValue().build();
    runtime.start(false);
    LOG.info("Dapr Workflow runtime started");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("Shutting down Dapr Workflow runtime");
      runtime.close();
    }));
  }
}
