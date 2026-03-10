package io.quarkiverse.dapr.examples;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that triggers a research workflow with tool calls routed through
 * Dapr Workflow Activities.
 * <p>
 * Each request:
 * <ol>
 *   <li>Starts a {@code SequentialOrchestrationWorkflow} (orchestration level).</li>
 *   <li>For the {@link ResearchWriter} sub-agent, starts an {@code AgentRunWorkflow}
 *       (per-agent level).</li>
 *   <li>Each LLM tool call ({@code getPopulation} / {@code getCapital}) is executed
 *       inside a {@code ToolCallActivity} (tool-call level).</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>
 * curl "http://localhost:8080/research?country=France"
 * curl "http://localhost:8080/research?country=Germany"
 * </pre>
 */
@Path("/research")
public class ResearchResource {

    @Inject
    ResearchWriter researchWriter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String research(
            @QueryParam("country") @DefaultValue("France") String country) {
        return researchWriter.research(country);
    }
}