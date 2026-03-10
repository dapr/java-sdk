package io.quarkiverse.dapr.examples;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that triggers the parallel creation workflow.
 * <p>
 * Runs {@link StoryCreator} (a nested {@code @SequenceAgent}) and {@link ResearchWriter}
 * in parallel via a {@code ParallelOrchestrationWorkflow} Dapr Workflow.
 * <p>
 * Example usage:
 * <pre>
 * curl "http://localhost:8080/parallel?topic=dragons&amp;country=France&amp;style=comedy"
 * </pre>
 */
@Path("/parallel")
public class ParallelResource {

    @Inject
    ParallelCreator parallelCreator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ParallelStatus create(
            @QueryParam("topic") @DefaultValue("dragons and wizards") String topic,
            @QueryParam("country") @DefaultValue("France") String country,
            @QueryParam("style") @DefaultValue("fantasy") String style) {
        return parallelCreator.create(topic, country, style);
    }
}