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
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.agentic.workflow.impl.SequentialAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow;

/**
 * Sequential agent service backed by a Dapr Workflow.
 * Extends {@link SequentialAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based sequential orchestration.
 */
public class DaprSequentialAgentService<T> extends SequentialAgentServiceImpl<T> implements DaprAgentService {

  private final DaprWorkflowClient workflowClient;

  /**
   * Creates a new DaprSequentialAgentService.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient the Dapr workflow client
   */
  public DaprSequentialAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
    super(agentServiceClass, resolveMethod(agentServiceClass));
    this.workflowClient = workflowClient;
  }

  private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
    if (agentServiceClass == UntypedAgent.class) {
      return null;
    }
    return AgentUtil.validateAgentClass(agentServiceClass, false, SequenceAgent.class);
  }

  @Override
  public String workflowType() {
    return SequentialOrchestrationWorkflow.class.getCanonicalName();
  }

  @Override
  public T build() {
    return build(() -> new DaprWorkflowPlanner(
        SequentialOrchestrationWorkflow.class,
        "Sequential",
        AgenticSystemTopology.SEQUENCE,
        workflowClient));
  }

  /**
   * Creates a builder for untyped agents.
   *
   * @param workflowClient the Dapr workflow client
   * @return a new DaprSequentialAgentService instance
   */
  public static DaprSequentialAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
    return new DaprSequentialAgentService<>(UntypedAgent.class, workflowClient);
  }

  /**
   * Creates a builder for typed agents.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient the Dapr workflow client
   * @param <T> the agent service type
   * @return a new DaprSequentialAgentService instance
   */
  public static <T> DaprSequentialAgentService<T> builder(Class<T> agentServiceClass,
      DaprWorkflowClient workflowClient) {
    return new DaprSequentialAgentService<>(agentServiceClass, workflowClient);
  }
}
