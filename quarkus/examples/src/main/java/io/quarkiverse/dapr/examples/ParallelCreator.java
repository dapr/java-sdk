package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.service.V;

/**
 * Composite agent that orchestrates {@link StoryCreator} and {@link ResearchWriter}
 * in parallel, backed by a Dapr Workflow.
 * <p>
 * Both sub-agents execute concurrently via a {@code ParallelOrchestrationWorkflow}.
 * {@link StoryCreator} is itself a {@code @SequenceAgent} that chains
 * {@link CreativeWriter} and {@link StyleEditor} — demonstrating nested composite agents.
 * Meanwhile {@link ResearchWriter} gathers facts about the country.
 */
public interface ParallelCreator {

    @ParallelAgent(name = "parallel-creator-agent",
            outputKey = "storyAndCountryResearch",
            subAgents = { StoryCreator.class, ResearchWriter.class })
    ParallelStatus create(@V("topic") String topic, @V("country") String country, @V("style") String style);

    @Output
    static ParallelStatus output(String story, String summary) {
      if(story == null || summary == null){
          return new ParallelStatus("ERROR", story, summary);
      }
      return new ParallelStatus("OK", story, summary);
    }
}