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
 * <p>
 * Obtains the {@link DaprWorkflowClient} from CDI to pass to each builder.
 */
public class DaprWorkflowAgentsBuilder implements WorkflowAgentsBuilder {

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
