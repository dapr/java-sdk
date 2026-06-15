package io.dapr.quarkus.examples;

import java.util.Map;
import java.util.Set;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Test profile for the durable tool-path test: swaps the plain {@link MockChatModel} for the
 * tool-driving {@link ToolCallingMockChatModel}.
 * <p>
 * Excludes {@link MockChatModel} (so there is no ambiguity) and enables the tool-calling
 * alternative. Isolated to tests annotated with this profile — the other examples tests keep
 * the plain mock.
 */
public class ToolCallingChatModelProfile implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(ToolCallingMockChatModel.class);
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.arc.exclude-types", "io.dapr.quarkus.examples.MockChatModel");
    }
}
