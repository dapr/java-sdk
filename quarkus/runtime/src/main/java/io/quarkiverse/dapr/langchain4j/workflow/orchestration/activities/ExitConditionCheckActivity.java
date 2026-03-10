package io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ExitConditionCheckInput;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr WorkflowActivity that checks the exit condition for loop workflows.
 * Returns {@code true} if the loop should exit, {@code false} otherwise.
 */
@ApplicationScoped
@ActivityMetadata(name = "exit-condition-check")
public class ExitConditionCheckActivity implements WorkflowActivity {

    @Override
    public Object run(WorkflowActivityContext ctx) {
        ExitConditionCheckInput input = ctx.getInput(ExitConditionCheckInput.class);
        DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
        if (planner == null) {
            throw new IllegalStateException("No planner found for ID: " + input.plannerId());
        }
        return planner.checkExitCondition(input.iteration());
    }
}
