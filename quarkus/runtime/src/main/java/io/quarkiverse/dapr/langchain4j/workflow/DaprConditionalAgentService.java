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

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.impl.ConditionalAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ConditionalOrchestrationWorkflow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Conditional agent service backed by a Dapr Workflow.
 * Extends {@link ConditionalAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based conditional orchestration.
 */
public class DaprConditionalAgentService<T> extends ConditionalAgentServiceImpl<T> implements DaprAgentService {

  private final DaprWorkflowClient workflowClient;
  private final Map<Integer, Predicate<AgenticScope>> daprConditions = new HashMap<>();
  private int agentCounter = 0;

  /**
   * Creates a new conditional agent service for the given agent service class.
   *
   * @param agentServiceClass the agent service class to create the service for
   * @param workflowClient the Dapr workflow client used for orchestration
   */
  public DaprConditionalAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
    super(agentServiceClass, resolveMethod(agentServiceClass));
    this.workflowClient = workflowClient;
  }

  private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
    if (agentServiceClass == UntypedAgent.class) {
      return null;
    }
    return AgentUtil.validateAgentClass(agentServiceClass, false, ConditionalAgent.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String workflowType() {
    return ConditionalOrchestrationWorkflow.class.getCanonicalName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprConditionalAgentService<T> subAgents(Predicate<AgenticScope> condition, Object... agents) {
    for (int i = 0; i < agents.length; i++) {
      daprConditions.put(agentCounter + i, condition);
    }
    agentCounter += agents.length;
    super.subAgents(condition, agents);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprConditionalAgentService<T> subAgents(String conditionDescription,
      Predicate<AgenticScope> condition, Object... agents) {
    for (int i = 0; i < agents.length; i++) {
      daprConditions.put(agentCounter + i, condition);
    }
    agentCounter += agents.length;
    super.subAgents(conditionDescription, condition, agents);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprConditionalAgentService<T> subAgent(Predicate<AgenticScope> condition, AgentExecutor agent) {
    daprConditions.put(agentCounter, condition);
    agentCounter++;
    super.subAgent(condition, agent);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DaprConditionalAgentService<T> subAgent(String conditionDescription,
      Predicate<AgenticScope> condition, AgentExecutor agent) {
    daprConditions.put(agentCounter, condition);
    agentCounter++;
    super.subAgent(conditionDescription, condition, agent);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T build() {
    return build(() -> {
      DaprWorkflowPlanner planner = new DaprWorkflowPlanner(
          ConditionalOrchestrationWorkflow.class,
          "Conditional",
          AgenticSystemTopology.ROUTER,
          workflowClient);
      planner.setConditions(daprConditions);
      return planner;
    });
  }

  /**
   * Creates a builder for an untyped conditional agent service.
   *
   * @param workflowClient the Dapr workflow client used for orchestration
   * @return a new untyped conditional agent service builder
   */
  public static DaprConditionalAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
    return new DaprConditionalAgentService<>(UntypedAgent.class, workflowClient);
  }

  /**
   * Creates a builder for a typed conditional agent service.
   *
   * @param <T> the agent service type
   * @param agentServiceClass the agent service class to create the builder for
   * @param workflowClient the Dapr workflow client used for orchestration
   * @return a new typed conditional agent service builder
   */
  public static <T> DaprConditionalAgentService<T> builder(Class<T> agentServiceClass,
      DaprWorkflowClient workflowClient) {
    return new DaprConditionalAgentService<>(agentServiceClass, workflowClient);
  }
}
