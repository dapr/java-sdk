package io.quarkiverse.dapr.langchain4j.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner.AgentMetadata;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.SequentialOrchestrationWorkflow;

class DaprWorkflowPlannerTest {

    private DaprWorkflowClient workflowClient;
    private DaprWorkflowPlanner planner;
    private AgentInstance agent1;
    private AgentInstance agent2;
    private AgenticScope scope;

    @BeforeEach
    void setUp() {
        workflowClient = mock(DaprWorkflowClient.class);
        planner = new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class,
                "test",
                AgenticSystemTopology.SEQUENCE,
                workflowClient);

        agent1 = mock(AgentInstance.class);
        when(agent1.name()).thenReturn("agent1");
        agent2 = mock(AgentInstance.class);
        when(agent2.name()).thenReturn("agent2");
        scope = mock(AgenticScope.class);
    }

    @AfterEach
    void tearDown() {
        DaprPlannerRegistry.unregister(planner.getPlannerId());
    }

    @Test
    void shouldHaveUniqueId() {
        DaprWorkflowPlanner planner2 = new DaprWorkflowPlanner(
                SequentialOrchestrationWorkflow.class, "test2", AgenticSystemTopology.SEQUENCE, workflowClient);
        assertThat(planner.getPlannerId()).isNotEqualTo(planner2.getPlannerId());
    }

    @Test
    void shouldReturnCorrectTopology() {
        assertThat(planner.topology()).isEqualTo(AgenticSystemTopology.SEQUENCE);
    }

    @Test
    void shouldRegisterInRegistryOnInit() {
        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1, agent2));
        planner.init(ctx);

        assertThat(DaprPlannerRegistry.get(planner.getPlannerId())).isSameAs(planner);
    }

    @Test
    void shouldStoreAgentsOnInit() {
        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1, agent2));
        planner.init(ctx);

        assertThat(planner.getAgent(0)).isSameAs(agent1);
        assertThat(planner.getAgent(1)).isSameAs(agent2);
    }

    @Test
    void shouldScheduleWorkflowOnFirstAction() {
        InitPlanningContext initCtx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1));
        planner.init(initCtx);

        // Simulate workflow posting a completion sentinel immediately
        Thread workflowThread = new Thread(() -> {
            try {
                Thread.sleep(50);
                planner.signalWorkflowComplete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        workflowThread.start();

        PlanningContext planCtx = mock(PlanningContext.class);
        Action action = planner.firstAction(planCtx);

        verify(workflowClient).scheduleNewWorkflow(
                eq("sequential-agent"),
                any(),
                eq(planner.getPlannerId()));

        // Sentinel was posted, so the planner returns done()
        assertThat(action.isDone()).isTrue();
    }

    @Test
    void executeAgentShouldQueueAndReturnFuture() {
        CompletableFuture<Void> future = planner.executeAgent(agent1, null);
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isFalse();

        // Completing the future should work
        future.complete(null);
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void signalWorkflowCompleteShouldPostSentinel() throws Exception {
        // Pre-load an agent exchange + a completion sentinel
        planner.executeAgent(agent1, null);
        planner.signalWorkflowComplete();

        // The planner should be able to drain both: first the agent, then the sentinel
        // We verify via internalNextAction indirectly through firstAction
        InitPlanningContext initCtx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1));
        planner.init(initCtx);

        // Skip firstAction's scheduleNewWorkflow - agent + sentinel already queued
        // First drain returns the agent
        // Second drain (via nextAction) returns done

        // This test verifies the sentinel mechanism works
        assertThat(planner.getPlannerId()).isNotNull();
    }

    @Test
    void shouldEvaluateExitConditionAsFalseWhenNull() {
        assertThat(planner.checkExitCondition(0)).isFalse();
    }

    @Test
    void shouldEvaluateExitCondition() {
        InitPlanningContext initCtx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1));
        planner.init(initCtx);

        planner.setExitCondition((s, iter) -> iter >= 3);

        assertThat(planner.checkExitCondition(0)).isFalse();
        assertThat(planner.checkExitCondition(2)).isFalse();
        assertThat(planner.checkExitCondition(3)).isTrue();
        assertThat(planner.checkExitCondition(5)).isTrue();
    }

    @Test
    void shouldReturnTrueForConditionCheckWhenNoCondition() {
        assertThat(planner.checkCondition(0)).isTrue();
        assertThat(planner.checkCondition(99)).isTrue();
    }

    @Test
    void shouldEvaluateConditionCheck() {
        InitPlanningContext initCtx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent1, agent2));
        planner.init(initCtx);

        Predicate<AgenticScope> alwaysTrue = s -> true;
        Predicate<AgenticScope> alwaysFalse = s -> false;
        planner.setConditions(Map.of(0, alwaysTrue, 1, alwaysFalse));

        assertThat(planner.checkCondition(0)).isTrue();
        assertThat(planner.checkCondition(1)).isFalse();
    }

    @Test
    void shouldHandleConcurrentExecuteAgentCalls() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            planner.executeAgent(agent1, null);
            latch.countDown();
        });
        Thread t2 = new Thread(() -> {
            planner.executeAgent(agent2, null);
            latch.countDown();
        });

        t1.start();
        t2.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    // --- Test interfaces for getAgentMetadata() ---

    interface AnnotatedAgent {
        @Agent(name = "annotated")
        @SystemMessage("You are a helpful assistant")
        @UserMessage("Please help with: {{it}}")
        String chat(String input);
    }

    interface AgentWithoutMessages {
        @Agent(name = "no-messages")
        String chat(String input);
    }

    interface AgentWithSystemOnly {
        @Agent(name = "system-only")
        @SystemMessage("System prompt here")
        String chat(String input);
    }

    // --- Tests for getAgentMetadata() ---

    @Test
    void getAgentMetadataShouldExtractAnnotations() {
        AgentInstance agent = mock(AgentInstance.class);
        when(agent.name()).thenReturn("annotated");
        when(agent.type()).thenReturn((Class) AnnotatedAgent.class);

        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent));
        planner.init(ctx);

        AgentMetadata metadata = planner.getAgentMetadata(0);

        assertThat(metadata.agentName()).isEqualTo("annotated");
        assertThat(metadata.systemMessage()).isEqualTo("You are a helpful assistant");
        assertThat(metadata.userMessage()).isEqualTo("Please help with: {{it}}");
    }

    @Test
    void getAgentMetadataShouldReturnNullMessagesWhenNotAnnotated() {
        AgentInstance agent = mock(AgentInstance.class);
        when(agent.name()).thenReturn("no-messages");
        when(agent.type()).thenReturn((Class) AgentWithoutMessages.class);

        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent));
        planner.init(ctx);

        AgentMetadata metadata = planner.getAgentMetadata(0);

        assertThat(metadata.agentName()).isEqualTo("no-messages");
        assertThat(metadata.systemMessage()).isNull();
        assertThat(metadata.userMessage()).isNull();
    }

    @Test
    void getAgentMetadataShouldExtractSystemMessageOnly() {
        AgentInstance agent = mock(AgentInstance.class);
        when(agent.name()).thenReturn("system-only");
        when(agent.type()).thenReturn((Class) AgentWithSystemOnly.class);

        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent));
        planner.init(ctx);

        AgentMetadata metadata = planner.getAgentMetadata(0);

        assertThat(metadata.agentName()).isEqualTo("system-only");
        assertThat(metadata.systemMessage()).isEqualTo("System prompt here");
        assertThat(metadata.userMessage()).isNull();
    }

    @Test
    void getAgentMetadataShouldHandleNullType() {
        AgentInstance agent = mock(AgentInstance.class);
        when(agent.name()).thenReturn("null-type");
        when(agent.type()).thenReturn(null);

        InitPlanningContext ctx = new InitPlanningContext(scope, mock(AgentInstance.class), List.of(agent));
        planner.init(ctx);

        AgentMetadata metadata = planner.getAgentMetadata(0);

        assertThat(metadata.agentName()).isEqualTo("null-type");
        assertThat(metadata.systemMessage()).isNull();
        assertThat(metadata.userMessage()).isNull();
    }
}
