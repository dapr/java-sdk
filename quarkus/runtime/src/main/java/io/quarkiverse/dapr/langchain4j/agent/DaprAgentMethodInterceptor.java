package io.quarkiverse.dapr.langchain4j.agent;

import java.lang.reflect.Method;
import java.util.UUID;

import org.jboss.logging.Logger;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentEvent;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunInput;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow;
import io.quarkiverse.dapr.langchain4j.workflow.WorkflowNameResolver;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that starts a Dapr {@link AgentRunWorkflow} for any standalone
 * {@code @Agent}-annotated method invocation.
 * <p>
 * <strong>Note:</strong> In practice this interceptor only fires when the {@code @Agent}
 * method belongs to a <em>regular CDI bean</em>. The quarkus-langchain4j agentic extension
 * registers {@code @Agent} interfaces as <em>synthetic beans</em> (via
 * {@code SyntheticBeanBuildItem}) without interception enabled, so this interceptor will
 * <em>not</em> fire for typical {@code @Agent} AiService calls.
 * <p>
 * For standalone {@code @Agent} calls, the workflow lifecycle is instead managed lazily by
 * {@link AgentRunLifecycleManager}, which is triggered from
 * {@link DaprToolCallInterceptor} on the first {@code @Tool} call of the request.
 * <p>
 * This class is retained for use cases where {@code @Agent} methods are declared on
 * regular CDI beans (not synthetic AiService beans), and for potential future quarkus-langchain4j
 * releases that enable interception on AiService synthetic beans.
 */
@DaprAgentInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class DaprAgentMethodInterceptor {

    private static final Logger LOG = Logger.getLogger(DaprAgentMethodInterceptor.class);

    @Inject
    DaprWorkflowClient workflowClient;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        // If already inside an orchestration-driven agent run (AgentExecutionActivity set this),
        // don't start another workflow — just proceed.
        if (DaprAgentContextHolder.get() != null) {
            return ctx.proceed();
        }

        // Standalone @Agent call — start a new AgentRunWorkflow for this invocation.
        String agentRunId = UUID.randomUUID().toString();
        Method method = ctx.getMethod();
        String agentName = extractAgentName(method, ctx.getTarget().getClass());
        String userMessage = extractUserMessageTemplate(method);
        String systemMessage = extractSystemMessageTemplate(method);

        LOG.infof("[AgentRun:%s] DaprAgentMethodInterceptor: starting AgentRunWorkflow for %s",
                agentRunId, agentName);

        AgentRunContext runContext = new AgentRunContext(agentRunId);
        DaprAgentRunRegistry.register(agentRunId, runContext);
        workflowClient.scheduleNewWorkflow(
                WorkflowNameResolver.resolve(AgentRunWorkflow.class),
                new AgentRunInput(agentRunId, agentName, userMessage, systemMessage), agentRunId);
        DaprAgentContextHolder.set(agentRunId);

        try {
            return ctx.proceed();
        } finally {
            LOG.infof("[AgentRun:%s] DaprAgentMethodInterceptor: @Agent method completed, sending done event", agentRunId);
            workflowClient.raiseEvent(agentRunId, "agent-event",
                    new AgentEvent("done", null, null, null));
            DaprAgentRunRegistry.unregister(agentRunId);
            DaprAgentContextHolder.clear();
        }
    }

    /**
     * Returns the {@code @Agent(name)} value if non-blank, otherwise falls back to
     * {@code DeclaringInterface.methodName} for CDI beans.
     */
    private String extractAgentName(Method method, Class<?> targetClass) {
        Agent agentAnnotation = method.getAnnotation(Agent.class);
        if (agentAnnotation != null && !agentAnnotation.name().isBlank()) {
            return agentAnnotation.name();
        }
        return targetClass.getSimpleName() + "." + method.getName();
    }

    /**
     * Returns the joined {@code @UserMessage} template text, or {@code null} if not present.
     */
    private String extractUserMessageTemplate(Method method) {
        UserMessage annotation = method.getAnnotation(UserMessage.class);
        if (annotation != null && annotation.value().length > 0) {
            return String.join("\n", annotation.value());
        }
        return null;
    }

    /**
     * Returns the joined {@code @SystemMessage} template text, or {@code null} if not present.
     */
    private String extractSystemMessageTemplate(Method method) {
        SystemMessage annotation = method.getAnnotation(SystemMessage.class);
        if (annotation != null && annotation.value().length > 0) {
            return String.join("\n", annotation.value());
        }
        return null;
    }
}