package io.quarkiverse.dapr.agents.registry.service;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * CDI bean implementing {@link TestAgent} so that {@link AgentRegistry}
 * can discover it via {@code BeanManager} during startup.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class TestAgentBean implements TestAgent {

    @Override
    public String chatWithPrompt() {
        return "mock response";
    }

    @Override
    public String chatSimple() {
        return "mock response";
    }

    @Override
    public String defaultNameAgent() {
        return "mock response";
    }
}
