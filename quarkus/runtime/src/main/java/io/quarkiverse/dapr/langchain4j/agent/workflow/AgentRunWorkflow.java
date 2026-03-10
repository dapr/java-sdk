package io.quarkiverse.dapr.langchain4j.agent.workflow;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import org.jboss.logging.Logger;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallActivity;
import io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallInput;
import io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallOutput;
import io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity;
import io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallInput;
import io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallOutput;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow representing the execution of a single {@code @Agent}-annotated method,
 * including all tool and LLM calls the agent makes during its ReAct loop.
 * <p>
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Started by {@link io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities.AgentExecutionActivity}
 *       (orchestration path) or lazily by {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunLifecycleManager}
 *       (standalone {@code @Agent} path) just before the agent is submitted.</li>
 *   <li>Loops waiting for {@code "agent-event"} external events raised by
 *       {@link io.quarkiverse.dapr.langchain4j.agent.DaprToolCallInterceptor} and
 *       {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}.</li>
 *   <li>For each {@code "tool-call"} event, schedules a {@link ToolCallActivity} that
 *       executes the tool on the Dapr activity thread and returns a {@link ToolCallOutput}.</li>
 *   <li>For each {@code "llm-call"} event, schedules a {@link LlmCallActivity} that
 *       executes the LLM call on the Dapr activity thread and returns a {@link LlmCallOutput}.</li>
 *   <li>After each activity, updates the Dapr custom status with an {@link AgentRunOutput}
 *       snapshot so observers can follow execution progress in real time.</li>
 *   <li>Terminates when a {@code "done"} event is received, setting the final
 *       {@link AgentRunOutput} as the custom status.</li>
 * </ol>
 */
@ApplicationScoped
@WorkflowMetadata(name = "agent")
public class AgentRunWorkflow implements Workflow {

    private static final Logger LOG = Logger.getLogger(AgentRunWorkflow.class);

    @Override
    public WorkflowStub create() {
        return ctx -> {
            AgentRunInput input = ctx.getInput(AgentRunInput.class);
            String agentRunId = input.agentRunId();
            String agentName = input.agentName();

            LOG.infof("[AgentRun:%s] AgentRunWorkflow started — agent=%s, userMessage=%s, systemMessage=%s",
                    agentRunId, agentName,
                    truncate(input.userMessage(), 120),
                    truncate(input.systemMessage(), 120));

            List<ToolCallOutput> toolCallOutputs = new ArrayList<>();
            List<LlmCallOutput> llmCallOutputs = new ArrayList<>();

            while (true) {
                // Wait for the next event from the agent thread or completion signal.
                AgentEvent event = ctx.waitForExternalEvent("agent-event", AgentEvent.class).await();

                LOG.infof("[AgentRun:%s] Received event: type=%s, callId=%s, name=%s",
                        agentRunId, event.type(), event.toolCallId(), event.toolName());

                if ("done".equals(event.type())) {
                    LOG.infof("[AgentRun:%s] AgentRunWorkflow completed — agent=%s, toolCalls=%d, llmCalls=%d",
                            agentRunId, agentName, toolCallOutputs.size(), llmCallOutputs.size());
                    break;
                }

                if ("tool-call".equals(event.type())) {
                    LOG.infof("[AgentRun:%s] Scheduling ToolCallActivity — tool=%s, args=%s",
                            agentRunId, event.toolName(), event.args());
                    ToolCallOutput toolOutput = ctx.callActivity(
                            "tool-call",
                            new ToolCallInput(agentRunId, event.toolCallId(), event.toolName(), event.args()),
                            ToolCallOutput.class).await();
                    toolCallOutputs.add(toolOutput);
                    LOG.infof("[AgentRun:%s] ToolCallActivity completed — tool=%s → %s",
                            agentRunId, event.toolName(), toolOutput.result());
                    ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
                }

                if ("llm-call".equals(event.type())) {
                    LOG.infof("[AgentRun:%s] Scheduling LlmCallActivity — method=%s",
                            agentRunId, event.toolName());
                    LlmCallOutput llmOutput = ctx.callActivity(
                            "llm-call",
                            new LlmCallInput(agentRunId, event.toolCallId(), event.toolName(), event.args()),
                            LlmCallOutput.class).await();
                    llmCallOutputs.add(llmOutput);
                    LOG.infof("[AgentRun:%s] LlmCallActivity completed — method=%s, response=%s",
                            agentRunId, event.toolName(), llmOutput.response());
                    ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
                }
            }

            // Set the final output so it is visible in the Dapr workflow dashboard.
            ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
        };
    }

    private static String truncate(String s, int maxLength) {
        if (s == null) {
            return null;
        }
        String trimmed = s.strip();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
    }
}
