package io.quarkiverse.dapr.examples;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that triggers the sequential story creation workflow.
 * <p>
 * Example usage:
 * <pre>
 * curl "http://localhost:8080/story?topic=dragons&style=comedy"
 * </pre>
 */
@Path("/story")
public class StoryResource {

    @Inject
    StoryCreator storyCreator;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String createStory(
            @QueryParam("topic") @DefaultValue("dragons and wizards") String topic,
            @QueryParam("style") @DefaultValue("fantasy") String style) {
        return storyCreator.write(topic, style);
    }
}
