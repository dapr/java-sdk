package io.quarkiverse.dapr.langchain4j.workflow;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.impl.LoopAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.LoopOrchestrationWorkflow;

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

    public DaprLoopAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
        super(agentServiceClass, resolveMethod(agentServiceClass));
        this.workflowClient = workflowClient;
    }

    private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
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

    @Override
    public DaprLoopAgentService<T> exitCondition(String description, Predicate<AgenticScope> exitCondition) {
        this.daprExitCondition = (scope, iter) -> exitCondition.test(scope);
        super.exitCondition(description, exitCondition);
        return this;
    }

    @Override
    public DaprLoopAgentService<T> exitCondition(String description, BiPredicate<AgenticScope, Integer> exitCondition) {
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

    public static DaprLoopAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
        return new DaprLoopAgentService<>(UntypedAgent.class, workflowClient);
    }

    public static <T> DaprLoopAgentService<T> builder(Class<T> agentServiceClass,
            DaprWorkflowClient workflowClient) {
        return new DaprLoopAgentService<>(agentServiceClass, workflowClient);
    }
}
