package io.quarkiverse.dapr.langchain4j.workflow;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.impl.ParallelAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ParallelOrchestrationWorkflow;

/**
 * Parallel agent service backed by a Dapr Workflow.
 * Extends {@link ParallelAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based parallel orchestration.
 */
public class DaprParallelAgentService<T> extends ParallelAgentServiceImpl<T> implements DaprAgentService {

    private final DaprWorkflowClient workflowClient;

    public DaprParallelAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
        super(agentServiceClass, resolveMethod(agentServiceClass));
        this.workflowClient = workflowClient;
    }

    private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
        if (agentServiceClass == UntypedAgent.class) {
            return null;
        }
        return AgentUtil.validateAgentClass(agentServiceClass, false, ParallelAgent.class);
    }

    @Override
    public String workflowType() {
        return ParallelOrchestrationWorkflow.class.getCanonicalName();
    }

    @Override
    public T build() {
        return build(() -> new DaprWorkflowPlanner(
                ParallelOrchestrationWorkflow.class,
                "Parallel",
                AgenticSystemTopology.PARALLEL,
                workflowClient));
    }

    public static DaprParallelAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
        return new DaprParallelAgentService<>(UntypedAgent.class, workflowClient);
    }

    public static <T> DaprParallelAgentService<T> builder(Class<T> agentServiceClass,
            DaprWorkflowClient workflowClient) {
        return new DaprParallelAgentService<>(agentServiceClass, workflowClient);
    }
}
