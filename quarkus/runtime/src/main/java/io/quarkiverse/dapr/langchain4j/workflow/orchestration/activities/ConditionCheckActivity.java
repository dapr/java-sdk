package io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ConditionCheckInput;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr WorkflowActivity that checks whether a conditional agent should execute.
 * Returns {@code true} if the agent's condition is met, {@code false} otherwise.
 */
@ApplicationScoped
@ActivityMetadata(name = "condition-check")
public class ConditionCheckActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
        ConditionCheckInput input = ctx.getInput(ConditionCheckInput.class);
        DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
        if (planner == null) {
            throw new IllegalStateException("No planner found for ID: " + input.plannerId());
        }
        return planner.checkCondition(input.agentIndex());
    }
}
