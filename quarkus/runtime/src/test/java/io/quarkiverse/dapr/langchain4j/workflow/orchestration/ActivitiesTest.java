package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ConditionCheckActivity;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.ExitConditionCheckActivity;

class ActivitiesTest {

    private DaprWorkflowPlanner planner;
    private AgentInstance agent1;
    private AgentInstance agent2;

    @BeforeEach
    void setUp() {
        DaprWorkflowClient client = mock(DaprWorkflowClient.class);
        planner = new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class, "test",
                AgenticSystemTopology.SEQUENCE, client);

        agent1 = mock(AgentInstance.class);
        when(agent1.name()).thenReturn("agent1");
        agent2 = mock(AgentInstance.class);
        when(agent2.name()).thenReturn("agent2");
        AgenticScope scope = mock(AgenticScope.class);

        InitPlanningContext initCtx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1, agent2));
        planner.init(initCtx);
    }

    @AfterEach
    void tearDown() {
        DaprPlannerRegistry.unregister(planner.getPlannerId());
    }

    @Test
    void agentExecutionActivityShouldSubmitAndReturnImmediately() {
        AgentExecutionActivity activity = new AgentExecutionActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(AgentExecInput.class))
                .thenReturn(new AgentExecInput(planner.getPlannerId(), 0,
                        planner.getPlannerId() + ":0"));

        // Activity should return immediately (non-blocking)
        Object result = activity.run(ctx);
        assertThat(result).isNull();
    }

    @Test
    void agentExecutionActivityShouldThrowForUnknownPlanner() {
        AgentExecutionActivity activity = new AgentExecutionActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(AgentExecInput.class))
                .thenReturn(new AgentExecInput("nonexistent-planner", 0, "nonexistent-planner:0"));

        assertThatThrownBy(() -> activity.run(ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No planner found");
    }

    @Test
    void exitConditionCheckActivityShouldReturnFalseWhenNoCondition() {
        ExitConditionCheckActivity activity = new ExitConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ExitConditionCheckInput.class))
                .thenReturn(new ExitConditionCheckInput(planner.getPlannerId(), 0));

        assertThat(activity.run(ctx)).isEqualTo(false);
    }

    @Test
    void exitConditionCheckActivityShouldEvaluateCondition() {
        planner.setExitCondition((s, iter) -> iter >= 2);

        ExitConditionCheckActivity activity = new ExitConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);

        when(ctx.getInput(ExitConditionCheckInput.class))
                .thenReturn(new ExitConditionCheckInput(planner.getPlannerId(), 1));
        assertThat(activity.run(ctx)).isEqualTo(false);

        when(ctx.getInput(ExitConditionCheckInput.class))
                .thenReturn(new ExitConditionCheckInput(planner.getPlannerId(), 2));
        assertThat(activity.run(ctx)).isEqualTo(true);
    }

    @Test
    void exitConditionCheckActivityShouldThrowForUnknownPlanner() {
        ExitConditionCheckActivity activity = new ExitConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ExitConditionCheckInput.class))
                .thenReturn(new ExitConditionCheckInput("nonexistent", 0));

        assertThatThrownBy(() -> activity.run(ctx))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void conditionCheckActivityShouldReturnTrueByDefault() {
        ConditionCheckActivity activity = new ConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ConditionCheckInput.class))
                .thenReturn(new ConditionCheckInput(planner.getPlannerId(), 0));

        assertThat(activity.run(ctx)).isEqualTo(true);
    }

    @Test
    void conditionCheckActivityShouldEvaluateCondition() {
        Predicate<AgenticScope> alwaysFalse = s -> false;
        planner.setConditions(Map.of(0, alwaysFalse));

        ConditionCheckActivity activity = new ConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ConditionCheckInput.class))
                .thenReturn(new ConditionCheckInput(planner.getPlannerId(), 0));

        assertThat(activity.run(ctx)).isEqualTo(false);
    }

    @Test
    void conditionCheckActivityShouldReturnTrueForAgentWithoutCondition() {
        Predicate<AgenticScope> alwaysFalse = s -> false;
        planner.setConditions(Map.of(0, alwaysFalse));

        ConditionCheckActivity activity = new ConditionCheckActivity();

        // Agent index 1 has no condition mapped
        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ConditionCheckInput.class))
                .thenReturn(new ConditionCheckInput(planner.getPlannerId(), 1));

        assertThat(activity.run(ctx)).isEqualTo(true);
    }

    @Test
    void conditionCheckActivityShouldThrowForUnknownPlanner() {
        ConditionCheckActivity activity = new ConditionCheckActivity();

        WorkflowActivityContext ctx = mock(WorkflowActivityContext.class);
        when(ctx.getInput(ConditionCheckInput.class))
                .thenReturn(new ConditionCheckInput("nonexistent", 0));

        assertThatThrownBy(() -> activity.run(ctx))
                .isInstanceOf(IllegalStateException.class);
    }
}