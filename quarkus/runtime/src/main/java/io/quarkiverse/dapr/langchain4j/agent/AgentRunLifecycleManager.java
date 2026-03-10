package io.quarkiverse.dapr.langchain4j.agent;

import java.util.UUID;

import org.jboss.logging.Logger;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentEvent;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunInput;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow;
import io.quarkiverse.dapr.langchain4j.workflow.WorkflowNameResolver;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Request-scoped CDI bean that manages the lifecycle of a lazily-started
 * {@link AgentRunWorkflow} for standalone {@code @Agent} invocations.
 * <p>
 * <h3>Why this exists</h3>
 * {@code @Agent} interfaces in quarkus-langchain4j are registered as <em>synthetic beans</em>
 * (via {@code SyntheticBeanBuildItem}) without interception enabled. This means CDI interceptors
 * such as {@code DaprAgentMethodInterceptor} cannot fire on {@code @Agent} method calls.
 * <p>
 * Instead, {@link DaprToolCallInterceptor} calls {@link #getOrActivate()} on the <em>first</em>
 * {@code @Tool} method call it intercepts within a request that has no active Dapr agent context.
 * This lazily starts the {@link AgentRunWorkflow} and sets {@link DaprAgentContextHolder} so
 * that all subsequent tool calls within the same request are also routed through Dapr.
 * <p>
 * When the CDI request scope is destroyed (i.e., after the HTTP response is sent), {@link #cleanup()}
 * sends the {@code "done"} event that terminates the {@link AgentRunWorkflow}.
 */
@RequestScoped
public class AgentRunLifecycleManager {

    private static final Logger LOG = Logger.getLogger(AgentRunLifecycleManager.class);

    @Inject
    DaprWorkflowClient workflowClient;

    private String agentRunId;

    /**
     * Returns the active agent run ID for this request, lazily starting an
     * {@link AgentRunWorkflow} if one has not been created yet.
     * <p>
     * This overload accepts the agent name and prompt metadata extracted from the
     * {@code @Agent}, {@code @UserMessage}, and {@code @SystemMessage} annotations (CDI bean
     * path) or from the rendered {@code ChatRequest} messages (AiService path).
     *
     * @param agentName     the value of {@code @Agent(name)}, or {@code null} / blank to use
     *                      {@code "standalone"}
     * @param userMessage   the user-message template or rendered text; may be {@code null}
     * @param systemMessage the system-message template or rendered text; may be {@code null}
     */
    public String getOrActivate(String agentName, String userMessage, String systemMessage) {
        if (agentRunId == null) {
            agentRunId = UUID.randomUUID().toString();
            String name = (agentName != null && !agentName.isBlank()) ? agentName : "standalone";
            AgentRunContext runContext = new AgentRunContext(agentRunId);
            DaprAgentRunRegistry.register(agentRunId, runContext);
            workflowClient.scheduleNewWorkflow(
                    WorkflowNameResolver.resolve(AgentRunWorkflow.class),
                    new AgentRunInput(agentRunId, name, userMessage, systemMessage), agentRunId);
            DaprAgentContextHolder.set(agentRunId);
            LOG.infof("[AgentRun:%s] AgentRunWorkflow started (lazy — standalone @Agent), agent=%s",
                    agentRunId, name);
        }
        return agentRunId;
    }

    /**
     * Returns the active agent run ID for this request, lazily starting an
     * {@link AgentRunWorkflow} if one has not been created yet.
     * <p>
     * Uses {@code "standalone"} as the agent name and {@code null} for prompt metadata.
     * Prefer {@link #getOrActivate(String, String, String)} when agent metadata is available.
     */
    public String getOrActivate() {
        return getOrActivate(null, null, null);
    }

    /**
     * Signals the active {@link AgentRunWorkflow} that the {@code @Agent} method has finished,
     * then unregisters the run and clears the context holder.
     * <p>
     * Called directly by the <em>generated CDI decorator</em> when the {@code @Agent} method
     * exits (successfully or via exception). Setting {@code agentRunId} to {@code null} afterward
     * makes {@link #cleanup()} a no-op, preventing a duplicate {@code "done"} event.
     * <p>
     * When no decorator was generated (e.g., the lazy-activation fallback path used by
     * {@link DaprChatModelDecorator}), this method is called by {@link #cleanup()} when the
     * CDI request scope ends.
     */
    public void triggerDone() {
        if (agentRunId != null) {
            LOG.infof("[AgentRun:%s] @Agent method exited — sending done event to AgentRunWorkflow", agentRunId);
            try {
                workflowClient.raiseEvent(agentRunId, "agent-event",
                        new AgentEvent("done", null, null, null));
            } finally {
                DaprAgentRunRegistry.unregister(agentRunId);
                DaprAgentContextHolder.clear();
                agentRunId = null; // prevents @PreDestroy from firing a second time
            }
        }
    }

    /**
     * Safety-net called when the CDI request scope is destroyed.
     * <p>
     * In the normal flow the generated CDI decorator already called {@link #triggerDone()},
     * so {@code agentRunId} is {@code null} and this method is a no-op. It only fires the
     * {@code "done"} event when the lazy-activation fallback path was used (i.e., no decorator
     * was present for this {@code @Agent} interface).
     */
    @PreDestroy
    void cleanup() {
        triggerDone();
    }
}
