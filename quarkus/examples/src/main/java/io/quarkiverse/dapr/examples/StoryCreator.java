package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

/**
 * Composite agent that orchestrates {@link CreativeWriter} and {@link StyleEditor}
 * in sequence, backed by a Dapr Workflow.
 * <p>
 * Uses {@code @SequenceAgent} which discovers the {@code DaprWorkflowAgentsBuilder}
 * via Java SPI to create the Dapr Workflow-based sequential orchestration.
 */
public interface StoryCreator {

    @SequenceAgent(name= "story-creator-agent",
            outputKey = "story",
            subAgents = { CreativeWriter.class, StyleEditor.class })
    String write(@V("topic") String topic, @V("style") String style);
}
