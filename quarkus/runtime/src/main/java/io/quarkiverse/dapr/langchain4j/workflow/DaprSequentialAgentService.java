package io.quarkiverse.dapr.langchain4j.workflow;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.agentic.workflow.impl.SequentialAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow;

/**
 * Sequential agent service backed by a Dapr Workflow.
 * Extends {@link SequentialAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based sequential orchestration.
 */
public class DaprSequentialAgentService<T> extends SequentialAgentServiceImpl<T> implements DaprAgentService {

    private final DaprWorkflowClient workflowClient;

    public DaprSequentialAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
        super(agentServiceClass, resolveMethod(agentServiceClass));
        this.workflowClient = workflowClient;
    }

    private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
        if (agentServiceClass == UntypedAgent.class) {
            return null;
        }
        return AgentUtil.validateAgentClass(agentServiceClass, false, SequenceAgent.class);
    }

    @Override
    public String workflowType() {
        return SequentialOrchestrationWorkflow.class.getCanonicalName();
    }

    @Override
    public T build() {
        return build(() -> new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class,
                "Sequential",
                AgenticSystemTopology.SEQUENCE,
                workflowClient));
    }

    public static DaprSequentialAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
        return new DaprSequentialAgentService<>(UntypedAgent.class, workflowClient);
    }

    public static <T> DaprSequentialAgentService<T> builder(Class<T> agentServiceClass,
            DaprWorkflowClient workflowClient) {
        return new DaprSequentialAgentService<>(agentServiceClass, workflowClient);
    }
}
