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
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;
import io.dapr.workflows.client.DaprWorkflowClient;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Dapr Workflow-backed implementation of {@link WorkflowAgentsBuilder}.
 * Discovered via Java SPI to provide Dapr-based agent service builders
 * for {@code @SequenceAgent}, {@code @ParallelAgent}, etc.
 *
 * <p>Obtains the {@link DaprWorkflowClient} from CDI to pass to each builder.
 */
public class DaprWorkflowAgentsBuilder implements WorkflowAgentsBuilder {

  /**
   * Retrieves the DaprWorkflowClient from CDI.
   *
   * @return the workflow client
   */
  private DaprWorkflowClient getWorkflowClient() {
    return CDI.current().select(DaprWorkflowClient.class).get();
  }

  @Override
  public SequentialAgentService<UntypedAgent> sequenceBuilder() {
    return DaprSequentialAgentService.builder(getWorkflowClient());
  }

  @Override
  public <T> SequentialAgentService<T> sequenceBuilder(Class<T> agentServiceClass) {
    return DaprSequentialAgentService.builder(agentServiceClass, getWorkflowClient());
  }

  @Override
  public ParallelAgentService<UntypedAgent> parallelBuilder() {
    return DaprParallelAgentService.builder(getWorkflowClient());
  }

  @Override
  public <T> ParallelAgentService<T> parallelBuilder(Class<T> agentServiceClass) {
    return DaprParallelAgentService.builder(agentServiceClass, getWorkflowClient());
  }

  @Override
  public LoopAgentService<UntypedAgent> loopBuilder() {
    return DaprLoopAgentService.builder(getWorkflowClient());
  }

  @Override
  public <T> LoopAgentService<T> loopBuilder(Class<T> agentServiceClass) {
    return DaprLoopAgentService.builder(agentServiceClass, getWorkflowClient());
  }

  @Override
  public ConditionalAgentService<UntypedAgent> conditionalBuilder() {
    return DaprConditionalAgentService.builder(getWorkflowClient());
  }

  @Override
  public <T> ConditionalAgentService<T> conditionalBuilder(Class<T> agentServiceClass) {
    return DaprConditionalAgentService.builder(agentServiceClass, getWorkflowClient());
  }
}
