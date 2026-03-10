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
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.impl.LoopAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.LoopOrchestrationWorkflow;

import java.lang.reflect.Method;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Loop agent service backed by a Dapr Workflow.
 * Extends {@link LoopAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based loop orchestration with configurable exit conditions.
 */
public class DaprLoopAgentService<T> extends LoopAgentServiceImpl<T> implements DaprAgentService {

  private final DaprWorkflowClient workflowClient;
  private int daprMaxIterations = Integer.MAX_VALUE;
  private BiPredicate<AgenticScope, Integer> daprExitCondition;
  private boolean daprTestExitAtLoopEnd;

  /**
   * Constructs a new DaprLoopAgentService.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient    the Dapr workflow client
   */
  public DaprLoopAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
    super(agentServiceClass, resolveMethod(agentServiceClass));
    this.workflowClient = workflowClient;
  }

  private static <T> Method resolveMethod(Class<T> agentServiceClass) {
    if (agentServiceClass == UntypedAgent.class) {
      return null;
    }
    return AgentUtil.validateAgentClass(agentServiceClass, false, LoopAgent.class);
  }

  @Override
  public String workflowType() {
    return LoopOrchestrationWorkflow.class.getCanonicalName();
  }

  @Override
  public DaprLoopAgentService<T> maxIterations(int maxIterations) {
    this.daprMaxIterations = maxIterations;
    super.maxIterations(maxIterations);
    return this;
  }

  @Override
  public DaprLoopAgentService<T> exitCondition(Predicate<AgenticScope> exitCondition) {
    this.daprExitCondition = (scope, iter) -> exitCondition.test(scope);
    super.exitCondition(exitCondition);
    return this;
  }

  @Override
  public DaprLoopAgentService<T> exitCondition(BiPredicate<AgenticScope, Integer> exitCondition) {
    this.daprExitCondition = exitCondition;
    super.exitCondition(exitCondition);
    return this;
  }

  /**
   * Sets the exit condition with a description.
   *
   * @param description   the description
   * @param exitCondition the exit condition predicate
   * @return this builder
   */
  @Override
  public DaprLoopAgentService<T> exitCondition(String description, Predicate<AgenticScope> exitCondition) {
    this.daprExitCondition = (scope, iter) -> exitCondition.test(scope);
    super.exitCondition(description, exitCondition);
    return this;
  }

  /**
   * Sets the exit condition with a description and iteration count.
   *
   * @param description   the description
   * @param exitCondition the exit condition bi-predicate
   * @return this builder
   */
  @Override
  public DaprLoopAgentService<T> exitCondition(
      String description, BiPredicate<AgenticScope, Integer> exitCondition) {
    this.daprExitCondition = exitCondition;
    super.exitCondition(description, exitCondition);
    return this;
  }

  @Override
  public DaprLoopAgentService<T> testExitAtLoopEnd(boolean testExitAtLoopEnd) {
    this.daprTestExitAtLoopEnd = testExitAtLoopEnd;
    super.testExitAtLoopEnd(testExitAtLoopEnd);
    return this;
  }

  @Override
  public T build() {
    return build(() -> {
      DaprWorkflowPlanner planner = new DaprWorkflowPlanner(
          LoopOrchestrationWorkflow.class,
          "Loop",
          AgenticSystemTopology.LOOP,
          workflowClient);
      planner.setMaxIterations(daprMaxIterations);
      planner.setExitCondition(daprExitCondition);
      planner.setTestExitAtLoopEnd(daprTestExitAtLoopEnd);
      return planner;
    });
  }

  /**
   * Creates a builder for an untyped agent.
   *
   * @param workflowClient the Dapr workflow client
   * @return a new builder instance
   */
  public static DaprLoopAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
    return new DaprLoopAgentService<>(UntypedAgent.class, workflowClient);
  }

  /**
   * Creates a builder for a typed agent service.
   *
   * @param agentServiceClass the agent service class
   * @param workflowClient    the Dapr workflow client
   * @param <T>               the agent service type
   * @return a new builder instance
   */
  public static <T> DaprLoopAgentService<T> builder(Class<T> agentServiceClass,
      DaprWorkflowClient workflowClient) {
    return new DaprLoopAgentService<>(agentServiceClass, workflowClient);
  }
}
