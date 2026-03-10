package io.quarkiverse.dapr.langchain4j.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ServiceLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;
import io.dapr.workflows.client.DaprWorkflowClient;

class DaprWorkflowAgentsBuilderTest {

    private DaprWorkflowClient workflowClient;

    @BeforeEach
    void setUp() {
        workflowClient = mock(DaprWorkflowClient.class);
    }

    @Test
    void shouldBeDiscoverableViaSPI() {
        ServiceLoader<WorkflowAgentsBuilder> loader = ServiceLoader.load(WorkflowAgentsBuilder.class);
        boolean found = false;
        for (WorkflowAgentsBuilder builder : loader) {
            if (builder instanceof DaprWorkflowAgentsBuilder) {
                found = true;
                break;
            }
        }
        assertThat(found).as("DaprWorkflowAgentsBuilder should be discoverable via ServiceLoader").isTrue();
    }

    @Test
    void sequenceBuilderShouldReturnDaprSequentialAgentService() {
        SequentialAgentService<UntypedAgent> service = DaprSequentialAgentService.builder(workflowClient);
        assertThat(service).isInstanceOf(DaprSequentialAgentService.class);
    }

    @Test
    void typedSequenceBuilderShouldReturnDaprSequentialAgentService() {
        SequentialAgentService<MyAgentService> service = DaprSequentialAgentService.builder(MyAgentService.class,
                workflowClient);
        assertThat(service).isInstanceOf(DaprSequentialAgentService.class);
    }

    @Test
    void parallelBuilderShouldReturnDaprParallelAgentService() {
        ParallelAgentService<UntypedAgent> service = DaprParallelAgentService.builder(workflowClient);
        assertThat(service).isInstanceOf(DaprParallelAgentService.class);
    }

    @Test
    void typedParallelBuilderShouldReturnDaprParallelAgentService() {
        ParallelAgentService<MyAgentService> service = DaprParallelAgentService.builder(MyAgentService.class,
                workflowClient);
        assertThat(service).isInstanceOf(DaprParallelAgentService.class);
    }

    @Test
    void loopBuilderShouldReturnDaprLoopAgentService() {
        LoopAgentService<UntypedAgent> service = DaprLoopAgentService.builder(workflowClient);
        assertThat(service).isInstanceOf(DaprLoopAgentService.class);
    }

    @Test
    void typedLoopBuilderShouldReturnDaprLoopAgentService() {
        LoopAgentService<MyAgentService> service = DaprLoopAgentService.builder(MyAgentService.class, workflowClient);
        assertThat(service).isInstanceOf(DaprLoopAgentService.class);
    }

    @Test
    void conditionalBuilderShouldReturnDaprConditionalAgentService() {
        ConditionalAgentService<UntypedAgent> service = DaprConditionalAgentService.builder(workflowClient);
        assertThat(service).isInstanceOf(DaprConditionalAgentService.class);
    }

    @Test
    void typedConditionalBuilderShouldReturnDaprConditionalAgentService() {
        ConditionalAgentService<MyAgentService> service = DaprConditionalAgentService.builder(MyAgentService.class,
                workflowClient);
        assertThat(service).isInstanceOf(DaprConditionalAgentService.class);
    }

    /** Dummy interface for typed builder tests. */
    interface MyAgentService {
    }
}
