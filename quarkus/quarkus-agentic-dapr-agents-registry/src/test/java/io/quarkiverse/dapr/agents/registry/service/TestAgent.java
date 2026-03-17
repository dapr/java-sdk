package io.quarkiverse.dapr.agents.registry.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;

/**
 * Test agent interface for verifying auto-discovery by {@link AgentRegistry}.
 * Contains multiple {@code @Agent} methods covering different metadata combinations.
 * <p>
 * Methods have no parameters to avoid validation errors from the langchain4j
 * AgenticProcessor which expects parameters to resolve from agent output keys.
 */
public interface TestAgent {

    @Agent(name = "test-agent-with-prompt", description = "Agent with system prompt")
    @SystemMessage("You are a test agent for integration testing.")
    String chatWithPrompt();

    @Agent(name = "test-agent-simple", description = "Simple agent without prompt")
    String chatSimple();

    @Agent(description = "Agent with default name")
    String defaultNameAgent();
}
