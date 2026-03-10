package io.quarkiverse.dapr.agents.registry.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.dapr.agents.registry.model.AgentMetadataSchema;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRegistryTest {

    private static final String APP_ID = "test-app";

    // --- Test interfaces ---
    // Note: @Agent methods use no parameters to avoid validation errors from
    // the langchain4j AgenticProcessor deployment step during @QuarkusTest runs.

    interface SimpleAgent {
        @Agent(name = "my-agent", description = "A simple agent")
        String chat();
    }

    interface AgentWithPrompts {
        @Agent(name = "prompted-agent", description = "Agent with prompts")
        @SystemMessage("You are a helpful assistant.")
        String ask();
    }

    interface AgentWithDefaultName {
        @Agent(description = "Agent with no explicit name")
        String doWork();
    }

    interface NoAgentInterface {
        String regularMethod();
    }

    interface MultipleAgentMethods {
        @Agent(name = "agent-one", description = "First agent")
        String first();

        @Agent(name = "agent-two", description = "Second agent")
        @SystemMessage("You are agent two.")
        String second();
    }

    // --- Test annotation and interfaces for sub-agent discovery ---

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface MockSequenceAgent {
        Class<?>[] subAgents();
    }

    interface SubAgentA {
        @Agent(name = "sub-agent-a", description = "Sub-agent A")
        String run();
    }

    interface SubAgentB {
        @Agent(name = "sub-agent-b", description = "Sub-agent B")
        String run();
    }

    interface CompositeAgent {
        @MockSequenceAgent(subAgents = { SubAgentA.class, SubAgentB.class })
        String orchestrate();
    }

    // --- Tests ---

    @Test
    void simpleAgentDiscovery() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(SimpleAgent.class, APP_ID);

        assertThat(agents).hasSize(1);
        AgentMetadataSchema schema = agents.get(0);
        assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
        assertThat(schema.getName()).isEqualTo("my-agent");
        assertThat(schema.getAgent().getGoal()).isEqualTo("A simple agent");
        assertThat(schema.getAgent().getAppId()).isEqualTo(APP_ID);
        assertThat(schema.getAgent().getType()).isEqualTo("standalone");
        assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
        assertThat(schema.getAgent().getSystemPrompt()).isNull();
        assertThat(schema.getRegisteredAt()).isNotBlank();
    }

    @Test
    void agentWithPromptsExtractsSystemMessage() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(AgentWithPrompts.class, APP_ID);

        assertThat(agents).hasSize(1);
        AgentMetadataSchema schema = agents.get(0);
        assertThat(schema.getName()).isEqualTo("prompted-agent");
        assertThat(schema.getAgent().getGoal()).isEqualTo("Agent with prompts");
        assertThat(schema.getAgent().getSystemPrompt()).isEqualTo("You are a helpful assistant.");
        assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
    }

    @Test
    void agentWithDefaultNameFallsBackToClassAndMethod() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(AgentWithDefaultName.class, APP_ID);

        assertThat(agents).hasSize(1);
        AgentMetadataSchema schema = agents.get(0);
        assertThat(schema.getName()).isEqualTo("AgentWithDefaultName.doWork");
        assertThat(schema.getAgent().getGoal()).isEqualTo("Agent with no explicit name");
    }

    @Test
    void noAgentInterfaceReturnsEmptyList() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(NoAgentInterface.class, APP_ID);

        assertThat(agents).isEmpty();
    }

    @Test
    void multipleAgentMethodsDiscoveredSeparately() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(MultipleAgentMethods.class, APP_ID);

        assertThat(agents).hasSize(2);
        assertThat(agents).extracting(AgentMetadataSchema::getName)
                .containsExactlyInAnyOrder("agent-one", "agent-two");

        AgentMetadataSchema agentTwo = agents.stream()
                .filter(a -> "agent-two".equals(a.getName()))
                .findFirst().orElseThrow();
        assertThat(agentTwo.getAgent().getSystemPrompt()).isEqualTo("You are agent two.");
        assertThat(agentTwo.getAgent().getGoal()).isEqualTo("Second agent");

        AgentMetadataSchema agentOne = agents.stream()
                .filter(a -> "agent-one".equals(a.getName()))
                .findFirst().orElseThrow();
        assertThat(agentOne.getAgent().getSystemPrompt()).isNull();
        assertThat(agentOne.getAgent().getGoal()).isEqualTo("First agent");
    }

    @Test
    void allSchemasHaveCorrectVersionAndAppId() {
        List<AgentMetadataSchema> agents = AgentRegistry.scanForAgents(MultipleAgentMethods.class, APP_ID);

        for (AgentMetadataSchema schema : agents) {
            assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
            assertThat(schema.getAgent().getAppId()).isEqualTo(APP_ID);
            assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
            assertThat(schema.getAgent().getType()).isEqualTo("standalone");
        }
    }

    @Test
    void extractSubAgentClassesFromCompositeAnnotation() throws Exception {
        java.lang.annotation.Annotation ann = CompositeAgent.class
                .getDeclaredMethod("orchestrate")
                .getDeclaredAnnotations()[0]; // @MockSequenceAgent

        Class<?>[] subAgents = AgentRegistry.extractSubAgentClasses(ann);

        assertThat(subAgents).containsExactly(SubAgentA.class, SubAgentB.class);
    }

    @Test
    void extractSubAgentClassesReturnsEmptyForNonComposite() throws Exception {
        java.lang.annotation.Annotation ann = SimpleAgent.class
                .getDeclaredMethod("chat")
                .getDeclaredAnnotations()[0]; // @Agent

        Class<?>[] subAgents = AgentRegistry.extractSubAgentClasses(ann);

        assertThat(subAgents).isEmpty();
    }

    @Test
    void scanForAgentsDiscoverSubAgentInterfaces() {
        // SubAgentA and SubAgentB are not CDI beans, but their @Agent should be scannable
        List<AgentMetadataSchema> agentsA = AgentRegistry.scanForAgents(SubAgentA.class, APP_ID);
        List<AgentMetadataSchema> agentsB = AgentRegistry.scanForAgents(SubAgentB.class, APP_ID);

        assertThat(agentsA).hasSize(1);
        assertThat(agentsA.get(0).getName()).isEqualTo("sub-agent-a");

        assertThat(agentsB).hasSize(1);
        assertThat(agentsB.get(0).getName()).isEqualTo("sub-agent-b");
    }
}
