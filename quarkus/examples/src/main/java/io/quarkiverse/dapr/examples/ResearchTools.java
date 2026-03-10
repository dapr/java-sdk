package io.quarkiverse.dapr.examples;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI bean providing research tools for the {@link ResearchWriter} agent.
 * <p>
 * Because the {@code quarkus-agentic-dapr} extension automatically applies
 * {@code @DaprAgentToolInterceptorBinding} to all {@code @Tool}-annotated methods at
 * build time, every call to these methods that occurs inside a Dapr-backed agent run is
 * transparently routed through a {@code ToolCallActivity} Dapr Workflow Activity.
 * <p>
 * This means:
 * <ul>
 *   <li>Each tool call is recorded in the Dapr Workflow history.</li>
 *   <li>If the process crashes during a tool call, Dapr retries the activity automatically.</li>
 *   <li>No code changes are needed here — the routing is applied automatically.</li>
 * </ul>
 */
@ApplicationScoped
public class ResearchTools {

    @Tool("Looks up real-time population data for a given country")
    public String getPopulation(String country) {
        // In a real implementation this would call an external API.
        // Here we return a stub so the example runs without network access.
        return switch (country.toLowerCase()) {
            case "france" -> "France has approximately 68 million inhabitants (2024).";
            case "germany" -> "Germany has approximately 84 million inhabitants (2024).";
            case "japan" -> "Japan has approximately 124 million inhabitants (2024).";
            default -> country + " population data is not available in this demo.";
        };
    }

    @Tool("Returns the official capital city of a given country")
    public String getCapital(String country) {
        return switch (country.toLowerCase()) {
            case "france" -> "The capital of France is Paris.";
            case "germany" -> "The capital of Germany is Berlin.";
            case "japan" -> "The capital of Japan is Tokyo.";
            default -> "Capital city for " + country + " is not available in this demo.";
        };
    }
}