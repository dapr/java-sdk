package io.dapr.quarkus.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Drop-in proof for the control-inversion entry point (uniform): plain, unchanged agent
 * interfaces are injected and called normally, but their AiServices-built bean is replaced by a
 * durable-workflow proxy — a leaf {@link CreativeWriter} runs as {@code react-agent}, and a
 * composite {@link StoryCreator} ({@code @SequenceAgent}) runs as {@code durable-sequence} over
 * react-agent children. No code changes to the agents.
 * <p>
 * Uses {@link MockChatModel}; a non-blank result means the workflow ran and completed.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class DurableEntryPointTest {

    @Inject
    CreativeWriter creativeWriter;

    @Inject
    StoryCreator storyCreator;

    @Inject
    StoryRouter storyRouter;

    @Inject
    ResearchAndWrite researchAndWrite;

    @Inject
    LoopWriter loopWriter;

    @Inject
    ParallelCreator parallelCreator;

    @Test
    void leafAgentRunsAsReactAgentWorkflow() {
        String story = creativeWriter.generateStory("dragons");
        assertNotNull(story);
        assertFalse(story.isBlank(), "expected the durable react-agent workflow to return a story");
    }

    @Test
    void compositeRunsAsDurableSequenceWorkflow() {
        String story = storyCreator.write("dragons", "comedy");
        assertNotNull(story);
        assertFalse(story.isBlank(), "expected the durable-sequence workflow to return a story");
    }

    @Test
    void conditionalRunsAsDurableConditionalWorkflow() {
        String story = storyRouter.route("dragons");
        assertNotNull(story);
        assertFalse(story.isBlank(), "expected the durable-conditional workflow to return a story");
    }

    @Test
    void parallelWithOutputCombinerProducesCombinedResult() {
        String combined = researchAndWrite.run("dragons", "France");
        assertNotNull(combined);
        // The @Output combiner ran over both sub-agents' scope outputs (not an empty outputKey).
        assertTrue(combined.contains("STORY:") && combined.contains("RESEARCH:"),
                "expected the @Output combiner to merge both sub-agent outputs, got: " + combined);
    }

    @Test
    void loopRunsAsDurableLoopWorkflow() {
        String story = loopWriter.write("dragons", "comedy");
        assertNotNull(story);
        assertFalse(story.isBlank(), "expected the durable-loop workflow to return a story");
    }

    @Test
    void nestedCompositeRunsAsDurableTreeWithStructuredOutput() {
        // ParallelCreator (@ParallelAgent) nests StoryCreator (@SequenceAgent) as a sub-agent and
        // returns a ParallelStatus record via @Output. This exercises recursive dispatch
        // (durable-parallel -> child durable-sequence -> react-agents), scope propagation (the
        // nested "story" bubbles up to the @Output combiner), and structured return coercion.
        ParallelStatus status = parallelCreator.create("dragons", "France", "comedy");
        assertNotNull(status);
        assertEquals("OK", status.status());
        assertNotNull(status.story(), "nested @SequenceAgent output should propagate up");
        assertNotNull(status.summary());
    }
}
