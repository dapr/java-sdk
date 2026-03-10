package io.quarkiverse.dapr.langchain4j.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow;

class DaprPlannerRegistryTest {

    @AfterEach
    void cleanup() {
        // Clean up any registered planners
        DaprPlannerRegistry.unregister("test-id-1");
        DaprPlannerRegistry.unregister("test-id-2");
    }

    @Test
    void shouldRegisterAndRetrievePlanner() {
        DaprWorkflowClient client = mock(DaprWorkflowClient.class);
        DaprWorkflowPlanner planner = new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class, "test", AgenticSystemTopology.SEQUENCE, client);

        String id = planner.getPlannerId();
        DaprPlannerRegistry.register(id, planner);

        assertThat(DaprPlannerRegistry.get(id)).isSameAs(planner);
    }

    @Test
    void shouldReturnNullForUnknownId() {
        assertThat(DaprPlannerRegistry.get("nonexistent")).isNull();
    }

    @Test
    void shouldUnregisterPlanner() {
        DaprWorkflowClient client = mock(DaprWorkflowClient.class);
        DaprWorkflowPlanner planner = new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class, "test", AgenticSystemTopology.SEQUENCE, client);

        String id = planner.getPlannerId();
        DaprPlannerRegistry.register(id, planner);
        DaprPlannerRegistry.unregister(id);

        assertThat(DaprPlannerRegistry.get(id)).isNull();
    }
}
