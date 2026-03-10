package io.quarkiverse.dapr.langchain4j.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.AgentUtil;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.impl.ConditionalAgentServiceImpl;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ConditionalOrchestrationWorkflow;

/**
 * Conditional agent service backed by a Dapr Workflow.
 * Extends {@link ConditionalAgentServiceImpl} and implements {@link DaprAgentService}
 * to provide Dapr-based conditional orchestration.
 */
public class DaprConditionalAgentService<T> extends ConditionalAgentServiceImpl<T> implements DaprAgentService {

    private final DaprWorkflowClient workflowClient;
    private final Map<Integer, Predicate<AgenticScope>> daprConditions = new HashMap<>();
    private int agentCounter = 0;

    public DaprConditionalAgentService(Class<T> agentServiceClass, DaprWorkflowClient workflowClient) {
        super(agentServiceClass, resolveMethod(agentServiceClass));
        this.workflowClient = workflowClient;
    }

    private static <T> java.lang.reflect.Method resolveMethod(Class<T> agentServiceClass) {
        if (agentServiceClass == UntypedAgent.class) {
            return null;
        }
        return AgentUtil.validateAgentClass(agentServiceClass, false, ConditionalAgent.class);
    }

    @Override
    public String workflowType() {
        return ConditionalOrchestrationWorkflow.class.getCanonicalName();
    }

    @Override
    public DaprConditionalAgentService<T> subAgents(Predicate<AgenticScope> condition, Object... agents) {
        for (int i = 0; i < agents.length; i++) {
            daprConditions.put(agentCounter + i, condition);
        }
        agentCounter += agents.length;
        super.subAgents(condition, agents);
        return this;
    }

    @Override
    public DaprConditionalAgentService<T> subAgents(String conditionDescription,
            Predicate<AgenticScope> condition, Object... agents) {
        for (int i = 0; i < agents.length; i++) {
            daprConditions.put(agentCounter + i, condition);
        }
        agentCounter += agents.length;
        super.subAgents(conditionDescription, condition, agents);
        return this;
    }

    @Override
    public DaprConditionalAgentService<T> subAgent(Predicate<AgenticScope> condition, AgentExecutor agent) {
        daprConditions.put(agentCounter, condition);
        agentCounter++;
        super.subAgent(condition, agent);
        return this;
    }

    @Override
    public DaprConditionalAgentService<T> subAgent(String conditionDescription,
            Predicate<AgenticScope> condition, AgentExecutor agent) {
        daprConditions.put(agentCounter, condition);
        agentCounter++;
        super.subAgent(conditionDescription, condition, agent);
        return this;
    }

    @Override
    public T build() {
        return build(() -> {
            DaprWorkflowPlanner planner = new DaprWorkflowPlanner(
                    ConditionalOrchestrationWorkflow.class,
                    "Conditional",
                    AgenticSystemTopology.ROUTER,
                    workflowClient);
            planner.setConditions(daprConditions);
            return planner;
        });
    }

    public static DaprConditionalAgentService<UntypedAgent> builder(DaprWorkflowClient workflowClient) {
        return new DaprConditionalAgentService<>(UntypedAgent.class, workflowClient);
    }

    public static <T> DaprConditionalAgentService<T> builder(Class<T> agentServiceClass,
            DaprWorkflowClient workflowClient) {
        return new DaprConditionalAgentService<>(agentServiceClass, workflowClient);
    }
}
