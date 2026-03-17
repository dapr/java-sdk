/*
 * Copyright 2025 The Dapr Authors
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

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.impl.ParallelAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ParallelOrchestrationWorkflow;

/**
 * Parallel agent service backed by a Dapr Workflow.
 * Extends {@link ParallelAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based parallel orchestration.
 */
public class DaprParallelAgentService<T> extends ParallelAgentServiceImpl<T> implements DaprAgentService {

  private final DaprWorkflowClient workflowClient;

  /**
   * Creates a new DaprParallelAgentService.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient the Dapr workflow client
   */
  public DaprParallelAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
    super(agentServiceClass, resolveMethod(agentServiceClass));
    this.workflowClient = workflowClient;
  }

  private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
    if (agentServiceClass == UntypedAgent.class) {
      return null;
    }
    return AgentUtil.validateAgentClass(agentServiceClass, false, ParallelAgent.class);
  }

  @Override
  public String workflowType() {
    return ParallelOrchestrationWorkflow.class.getCanonicalName();
  }

  @Override
  public T build() {
    return build(() -> new DaprWorkflowPlanner(
        ParallelOrchestrationWorkflow.class,
        "Parallel",
        AgenticSystemTopology.PARALLEL,
        workflowClient));
  }

  /**
   * Creates a builder for untyped agents.
   *
   * @param workflowClient the Dapr workflow client
   * @return a new DaprParallelAgentService instance
   */
  public static DaprParallelAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
    return new DaprParallelAgentService<>(UntypedAgent.class, workflowClient);
  }

  /**
   * Creates a builder for typed agents.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient the Dapr workflow client
   * @param <T> the agent service type
   * @return a new DaprParallelAgentService instance
   */
  public static <T> DaprParallelAgentService<T> builder(Class<T> agentServiceClass,
      DaprWorkflowClient workflowClient) {
    return new DaprParallelAgentService<>(agentServiceClass, workflowClient);
  }
}
