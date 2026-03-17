package io.quarkiverse.dapr.agents.registry.service;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import io.quarkiverse.dapr.agents.registry.model.AgentMetadataSchema;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for {@link AgentRegistry} using Dapr dev services.
 * <p>
 * Requires Docker for Dapr dev services (starts daprd, placement, scheduler,
 * PostgreSQL state store, and dashboard containers via Testcontainers).
 * Uses {@link MockChatModel} instead of a real LLM.
 * Uses {@link TestAgentBean} to provide a CDI bean with {@code @Agent}-annotated
 * interface methods for the registry to discover.
 */
@QuarkusTest
class AgentRegistryDevServicesTest {

    private static final String STATE_STORE = "kvstore";
    private static final String TEAM = "test-team";
    private static final String APP_ID = "local-dapr-app";

    @Inject
    AgentRegistry registry;

    @Inject
    DaprClient daprClient;

    @Test
    void registryShouldBeInjectable() {
        assertThat(registry).isNotNull();
    }

    @Test
    void daprClientShouldBeInjectable() {
        assertThat(daprClient).isNotNull();
    }

    @Test
    void autoDiscoveredAgentWithPromptShouldBeInStateStore() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            State<AgentMetadataSchema> state = daprClient.getState(
                    STATE_STORE, "agents:" + TEAM + ":test-agent-with-prompt", AgentMetadataSchema.class).block();
            assertThat(state).isNotNull();
            assertThat(state.getValue()).isNotNull();

            AgentMetadataSchema schema = state.getValue();
            assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
            assertThat(schema.getName()).isEqualTo("test-agent-with-prompt");
            assertThat(schema.getAgent().getAppId()).isEqualTo(APP_ID);
            assertThat(schema.getAgent().getType()).isEqualTo("standalone");
            assertThat(schema.getAgent().getGoal()).isEqualTo("Agent with system prompt");
            assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
            assertThat(schema.getAgent().getSystemPrompt()).isEqualTo("You are a test agent for integration testing.");
            assertThat(schema.getRegisteredAt()).isNotBlank();
        });
    }

    @Test
    void autoDiscoveredSimpleAgentShouldBeInStateStore() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            State<AgentMetadataSchema> state = daprClient.getState(
                    STATE_STORE, "agents:" + TEAM + ":test-agent-simple", AgentMetadataSchema.class).block();
            assertThat(state).isNotNull();
            assertThat(state.getValue()).isNotNull();

            AgentMetadataSchema schema = state.getValue();
            assertThat(schema.getName()).isEqualTo("test-agent-simple");
            assertThat(schema.getAgent().getGoal()).isEqualTo("Simple agent without prompt");
            assertThat(schema.getAgent().getSystemPrompt()).isNull();
            assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
        });
    }

    @Test
    void agentWithDefaultNameShouldUseInterfaceAndMethodName() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            State<AgentMetadataSchema> state = daprClient.getState(
                    STATE_STORE, "agents:" + TEAM + ":TestAgent.defaultNameAgent", AgentMetadataSchema.class).block();
            assertThat(state).isNotNull();
            assertThat(state.getValue()).isNotNull();

            AgentMetadataSchema schema = state.getValue();
            assertThat(schema.getName()).isEqualTo("TestAgent.defaultNameAgent");
            assertThat(schema.getAgent().getGoal()).isEqualTo("Agent with default name");
        });
    }



    @Test
    void allAutoDiscoveredAgentsShouldHaveConsistentMetadata() {
        String[] expectedAgents = {"test-agent-with-prompt", "test-agent-simple", "TestAgent.defaultNameAgent"};

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            for (String agentName : expectedAgents) {
                State<AgentMetadataSchema> state = daprClient.getState(
                        STATE_STORE, "agents:" + TEAM + ":" + agentName, AgentMetadataSchema.class).block();
                assertThat(state.getValue())
                        .as("Agent '%s' should be registered", agentName)
                        .isNotNull();

                AgentMetadataSchema schema = state.getValue();
                assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
                assertThat(schema.getAgent().getAppId()).isEqualTo(APP_ID);
                assertThat(schema.getAgent().getType()).isEqualTo("standalone");
                assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
                assertThat(schema.getRegisteredAt()).isNotBlank();
            }
        });
    }
}
