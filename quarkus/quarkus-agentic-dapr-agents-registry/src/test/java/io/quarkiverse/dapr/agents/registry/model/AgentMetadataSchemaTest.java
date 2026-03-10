package io.quarkiverse.dapr.agents.registry.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentMetadataSchemaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildMinimalSchema() {
        AgentMetadataSchema schema = AgentMetadataSchema.builder()
                .schemaVersion("0.11.1")
                .name("test-agent")
                .registeredAt("2025-01-01T00:00:00Z")
                .agent(AgentMetadata.builder()
                        .appId("my-app")
                        .type("standalone")
                        .build())
                .build();

        assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
        assertThat(schema.getName()).isEqualTo("test-agent");
        assertThat(schema.getRegisteredAt()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(schema.getAgent().getAppId()).isEqualTo("my-app");
        assertThat(schema.getAgent().getType()).isEqualTo("standalone");
        assertThat(schema.getAgent().isOrchestrator()).isFalse();
        assertThat(schema.getAgent().getRole()).isEmpty();
        assertThat(schema.getAgent().getGoal()).isEmpty();
        assertThat(schema.getPubsub()).isNull();
        assertThat(schema.getMemory()).isNull();
        assertThat(schema.getLlm()).isNull();
        assertThat(schema.getRegistry()).isNull();
        assertThat(schema.getTools()).isNull();
        assertThat(schema.getMaxIterations()).isNull();
        assertThat(schema.getToolChoice()).isNull();
        assertThat(schema.getAgentMetadata()).isNull();
    }

    @Test
    void buildFullSchema() {
        AgentMetadataSchema schema = AgentMetadataSchema.builder()
                .schemaVersion("0.11.1")
                .name("orchestrator-agent")
                .registeredAt("2025-06-15T10:30:00Z")
                .agent(AgentMetadata.builder()
                        .appId("orch-app")
                        .type("durable")
                        .orchestrator(true)
                        .role("coordinator")
                        .goal("Coordinate tasks across agents")
                        .instructions(List.of("Be concise", "Delegate work"))
                        .statestore("statestore")
                        .systemPrompt("You are a coordinator agent.")
                        .framework("langchain4j")
                        .build())
                .pubsub(PubSubMetadata.builder()
                        .name("pubsub")
                        .broadcastTopic("broadcast")
                        .agentTopic("agent-messages")
                        .build())
                .memory(MemoryMetadata.builder()
                        .type("conversation")
                        .statestore("memory-store")
                        .build())
                .llm(LLMMetadata.builder()
                        .client("openai")
                        .provider("openai")
                        .api("chat")
                        .model("gpt-4")
                        .baseUrl("https://api.openai.com")
                        .promptTemplate("Answer: {input}")
                        .build())
                .registry(RegistryMetadata.builder()
                        .statestore("registry-store")
                        .name("team-registry")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("search")
                        .toolDescription("Search the web")
                        .toolArgs("{\"query\": \"string\"}")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("calculator")
                        .toolDescription("Perform calculations")
                        .toolArgs("{\"expression\": \"string\"}")
                        .build())
                .maxIterations(10)
                .toolChoice("auto")
                .agentMetadata(Map.of("version", "1.0", "team", "alpha"))
                .build();

        assertThat(schema.getAgent().isOrchestrator()).isTrue();
        assertThat(schema.getAgent().getRole()).isEqualTo("coordinator");
        assertThat(schema.getAgent().getGoal()).isEqualTo("Coordinate tasks across agents");
        assertThat(schema.getAgent().getInstructions()).containsExactly("Be concise", "Delegate work");
        assertThat(schema.getAgent().getStatestore()).isEqualTo("statestore");
        assertThat(schema.getAgent().getSystemPrompt()).isEqualTo("You are a coordinator agent.");
        assertThat(schema.getAgent().getFramework()).isEqualTo("langchain4j");
        assertThat(schema.getPubsub().getName()).isEqualTo("pubsub");
        assertThat(schema.getPubsub().getBroadcastTopic()).isEqualTo("broadcast");
        assertThat(schema.getPubsub().getAgentTopic()).isEqualTo("agent-messages");
        assertThat(schema.getMemory().getType()).isEqualTo("conversation");
        assertThat(schema.getMemory().getStatestore()).isEqualTo("memory-store");
        assertThat(schema.getLlm().getClient()).isEqualTo("openai");
        assertThat(schema.getLlm().getProvider()).isEqualTo("openai");
        assertThat(schema.getLlm().getApi()).isEqualTo("chat");
        assertThat(schema.getLlm().getModel()).isEqualTo("gpt-4");
        assertThat(schema.getLlm().getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(schema.getLlm().getPromptTemplate()).isEqualTo("Answer: {input}");
        assertThat(schema.getRegistry().getStatestore()).isEqualTo("registry-store");
        assertThat(schema.getRegistry().getName()).isEqualTo("team-registry");
        assertThat(schema.getTools()).hasSize(2);
        assertThat(schema.getTools().get(0).getToolName()).isEqualTo("search");
        assertThat(schema.getTools().get(1).getToolName()).isEqualTo("calculator");
        assertThat(schema.getMaxIterations()).isEqualTo(10);
        assertThat(schema.getToolChoice()).isEqualTo("auto");
        assertThat(schema.getAgentMetadata()).containsEntry("version", "1.0");
    }

    @Test
    void buildLlmWithAzureConfig() {
        LLMMetadata llm = LLMMetadata.builder()
                .client("azure-openai")
                .provider("azure")
                .azureEndpoint("https://my-resource.openai.azure.com")
                .azureDeployment("gpt-4-deployment")
                .componentName("llm-component")
                .build();

        assertThat(llm.getAzureEndpoint()).isEqualTo("https://my-resource.openai.azure.com");
        assertThat(llm.getAzureDeployment()).isEqualTo("gpt-4-deployment");
        assertThat(llm.getComponentName()).isEqualTo("llm-component");
        assertThat(llm.getApi()).isEqualTo("unknown");
        assertThat(llm.getModel()).isEqualTo("unknown");
    }

    @Test
    void buildLlmWithDefaults() {
        LLMMetadata llm = LLMMetadata.builder()
                .client("openai")
                .provider("openai")
                .build();

        assertThat(llm.getApi()).isEqualTo("unknown");
        assertThat(llm.getModel()).isEqualTo("unknown");
        assertThat(llm.getComponentName()).isNull();
        assertThat(llm.getBaseUrl()).isNull();
    }

    @Test
    void schemaBuilderRequiresAllRequiredFields() {
        assertThatThrownBy(() -> AgentMetadataSchema.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schemaVersion, agent, name, and registeredAt are required");
    }

    @Test
    void agentBuilderRequiresAppIdAndType() {
        assertThatThrownBy(() -> AgentMetadata.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("appId and type are required");
    }

    @Test
    void llmBuilderRequiresClientAndProvider() {
        assertThatThrownBy(() -> LLMMetadata.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client and provider are required");
    }

    @Test
    void memoryBuilderRequiresType() {
        assertThatThrownBy(() -> MemoryMetadata.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("type is required");
    }

    @Test
    void pubsubBuilderRequiresName() {
        assertThatThrownBy(() -> PubSubMetadata.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void toolBuilderRequiresAllFields() {
        assertThatThrownBy(() -> ToolMetadata.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("toolName, toolDescription, and toolArgs are required");
    }

    @Test
    void registryBuilderHasNoRequiredFields() {
        RegistryMetadata registry = RegistryMetadata.builder().build();
        assertThat(registry.getStatestore()).isNull();
        assertThat(registry.getName()).isNull();
    }

    @Test
    void serializeMinimalSchemaToJson() throws Exception {
        AgentMetadataSchema schema = AgentMetadataSchema.builder()
                .schemaVersion("0.11.1")
                .name("test-agent")
                .registeredAt("2025-01-01T00:00:00Z")
                .agent(AgentMetadata.builder()
                        .appId("my-app")
                        .type("standalone")
                        .build())
                .build();

        String json = mapper.writeValueAsString(schema);

        assertThat(json).contains("\"schema_version\":\"0.11.1\"");
        assertThat(json).contains("\"name\":\"test-agent\"");
        assertThat(json).contains("\"registered_at\":\"2025-01-01T00:00:00Z\"");
        assertThat(json).contains("\"appid\":\"my-app\"");
        assertThat(json).contains("\"type\":\"standalone\"");
        // null fields should be excluded
        assertThat(json).doesNotContain("\"pubsub\"");
        assertThat(json).doesNotContain("\"memory\"");
        assertThat(json).doesNotContain("\"llm\"");
        assertThat(json).doesNotContain("\"registry\"");
        assertThat(json).doesNotContain("\"tools\"");
        assertThat(json).doesNotContain("\"max_iterations\"");
        assertThat(json).doesNotContain("\"tool_choice\"");
        assertThat(json).doesNotContain("\"agent_metadata\"");
    }

    @Test
    void serializeAndDeserializeFullSchema() throws Exception {
        AgentMetadataSchema original = AgentMetadataSchema.builder()
                .schemaVersion("0.11.1")
                .name("round-trip-agent")
                .registeredAt("2025-06-15T10:30:00Z")
                .agent(AgentMetadata.builder()
                        .appId("rt-app")
                        .type("durable")
                        .orchestrator(true)
                        .role("planner")
                        .goal("Plan tasks")
                        .instructions(List.of("Step 1", "Step 2"))
                        .statestore("state-store")
                        .systemPrompt("You are a planner.")
                        .framework("quarkus")
                        .build())
                .pubsub(PubSubMetadata.builder()
                        .name("pubsub")
                        .broadcastTopic("broadcast")
                        .agentTopic("agent-topic")
                        .build())
                .memory(MemoryMetadata.builder()
                        .type("buffer")
                        .statestore("mem-store")
                        .build())
                .llm(LLMMetadata.builder()
                        .client("openai")
                        .provider("openai")
                        .api("chat")
                        .model("gpt-4o")
                        .baseUrl("https://api.openai.com")
                        .build())
                .registry(RegistryMetadata.builder()
                        .statestore("reg-store")
                        .name("my-registry")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("search")
                        .toolDescription("Web search")
                        .toolArgs("{}")
                        .build())
                .maxIterations(5)
                .toolChoice("auto")
                .agentMetadata(Map.of("env", "prod"))
                .build();

        String json = mapper.writeValueAsString(original);
        AgentMetadataSchema deserialized = mapper.readValue(json, AgentMetadataSchema.class);

        assertThat(deserialized.getSchemaVersion()).isEqualTo("0.11.1");
        assertThat(deserialized.getName()).isEqualTo("round-trip-agent");
        assertThat(deserialized.getRegisteredAt()).isEqualTo("2025-06-15T10:30:00Z");
        assertThat(deserialized.getAgent().getAppId()).isEqualTo("rt-app");
        assertThat(deserialized.getAgent().getType()).isEqualTo("durable");
        assertThat(deserialized.getAgent().isOrchestrator()).isTrue();
        assertThat(deserialized.getAgent().getRole()).isEqualTo("planner");
        assertThat(deserialized.getAgent().getGoal()).isEqualTo("Plan tasks");
        assertThat(deserialized.getAgent().getInstructions()).containsExactly("Step 1", "Step 2");
        assertThat(deserialized.getAgent().getStatestore()).isEqualTo("state-store");
        assertThat(deserialized.getAgent().getSystemPrompt()).isEqualTo("You are a planner.");
        assertThat(deserialized.getAgent().getFramework()).isEqualTo("quarkus");
        assertThat(deserialized.getPubsub().getName()).isEqualTo("pubsub");
        assertThat(deserialized.getPubsub().getBroadcastTopic()).isEqualTo("broadcast");
        assertThat(deserialized.getPubsub().getAgentTopic()).isEqualTo("agent-topic");
        assertThat(deserialized.getMemory().getType()).isEqualTo("buffer");
        assertThat(deserialized.getMemory().getStatestore()).isEqualTo("mem-store");
        assertThat(deserialized.getLlm().getClient()).isEqualTo("openai");
        assertThat(deserialized.getLlm().getModel()).isEqualTo("gpt-4o");
        assertThat(deserialized.getLlm().getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(deserialized.getRegistry().getStatestore()).isEqualTo("reg-store");
        assertThat(deserialized.getRegistry().getName()).isEqualTo("my-registry");
        assertThat(deserialized.getTools()).hasSize(1);
        assertThat(deserialized.getTools().get(0).getToolName()).isEqualTo("search");
        assertThat(deserialized.getMaxIterations()).isEqualTo(5);
        assertThat(deserialized.getToolChoice()).isEqualTo("auto");
        assertThat(deserialized.getAgentMetadata()).containsEntry("env", "prod");
    }

    @Test
    void deserializeFromJson() throws Exception {
        String json = """
                {
                  "schema_version": "0.11.1",
                  "name": "json-agent",
                  "registered_at": "2025-03-01T00:00:00Z",
                  "agent": {
                    "appid": "json-app",
                    "type": "standalone",
                    "orchestrator": false,
                    "role": "worker",
                    "goal": "Process tasks",
                    "instructions": ["Follow orders"],
                    "system_prompt": "You are a worker.",
                    "framework": "dapr"
                  },
                  "llm": {
                    "client": "anthropic",
                    "provider": "anthropic",
                    "model": "claude-3"
                  },
                  "tools": [
                    {
                      "tool_name": "fetch",
                      "tool_description": "Fetch URL",
                      "tool_args": "{\\"url\\": \\"string\\"}"
                    }
                  ],
                  "max_iterations": 20,
                  "tool_choice": "required"
                }
                """;

        AgentMetadataSchema schema = mapper.readValue(json, AgentMetadataSchema.class);

        assertThat(schema.getSchemaVersion()).isEqualTo("0.11.1");
        assertThat(schema.getName()).isEqualTo("json-agent");
        assertThat(schema.getAgent().getAppId()).isEqualTo("json-app");
        assertThat(schema.getAgent().getType()).isEqualTo("standalone");
        assertThat(schema.getAgent().isOrchestrator()).isFalse();
        assertThat(schema.getAgent().getRole()).isEqualTo("worker");
        assertThat(schema.getAgent().getGoal()).isEqualTo("Process tasks");
        assertThat(schema.getAgent().getInstructions()).containsExactly("Follow orders");
        assertThat(schema.getAgent().getSystemPrompt()).isEqualTo("You are a worker.");
        assertThat(schema.getAgent().getFramework()).isEqualTo("dapr");
        assertThat(schema.getLlm().getClient()).isEqualTo("anthropic");
        assertThat(schema.getLlm().getProvider()).isEqualTo("anthropic");
        assertThat(schema.getLlm().getModel()).isEqualTo("claude-3");
        assertThat(schema.getTools()).hasSize(1);
        assertThat(schema.getTools().get(0).getToolName()).isEqualTo("fetch");
        assertThat(schema.getMaxIterations()).isEqualTo(20);
        assertThat(schema.getToolChoice()).isEqualTo("required");
        assertThat(schema.getPubsub()).isNull();
        assertThat(schema.getMemory()).isNull();
        assertThat(schema.getRegistry()).isNull();
    }

    @Test
    void addToolIncrementally() {
        AgentMetadataSchema schema = AgentMetadataSchema.builder()
                .schemaVersion("0.11.1")
                .name("tool-agent")
                .registeredAt("2025-01-01T00:00:00Z")
                .agent(AgentMetadata.builder()
                        .appId("tool-app")
                        .type("standalone")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("t1")
                        .toolDescription("First tool")
                        .toolArgs("{}")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("t2")
                        .toolDescription("Second tool")
                        .toolArgs("{}")
                        .build())
                .addTool(ToolMetadata.builder()
                        .toolName("t3")
                        .toolDescription("Third tool")
                        .toolArgs("{}")
                        .build())
                .build();

        assertThat(schema.getTools()).hasSize(3);
        assertThat(schema.getTools()).extracting(ToolMetadata::getToolName)
                .containsExactly("t1", "t2", "t3");
    }
}