package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Sub-agent that writes a short research summary about a country by calling tools.
 * <p>
 * The {@link ToolBox} annotation tells quarkus-langchain4j to make {@link ResearchTools}
 * available to the LLM for this agent's method. When the LLM decides to call
 * {@code getPopulation} or {@code getCapital}, the call is intercepted by the Dapr
 * extension and executed inside a {@code ToolCallActivity} Dapr Workflow Activity —
 * providing a durable, auditable record of every tool invocation.
 * <p>
 * <b>Architecture note:</b> No changes are required in this interface to enable the
 * Dapr routing. The {@code quarkus-agentic-dapr} deployment module applies
 * {@code @DaprAgentToolInterceptorBinding} to all {@code @Tool}-annotated methods at
 * build time, and {@code DaprWorkflowPlanner} sets the per-agent context on the
 * executing thread before the agent starts.
 */
public interface ResearchWriter {

    @ToolBox(ResearchTools.class)
    @UserMessage("""
            You are a research assistant.
            Write a concise 2-sentence summary about the country {{country}}
            using the available tools to fetch accurate data.
            Return only the summary.
            """)
    @Agent(name = "research-location-agent",
        description = "Researches and summarises facts about a country", outputKey = "summary")
    String research(@V("country") String country);
}