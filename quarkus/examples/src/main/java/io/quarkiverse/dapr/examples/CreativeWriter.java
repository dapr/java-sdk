package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Sub-agent that generates a creative story draft based on a given topic.
 */
public interface CreativeWriter {

    @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than 3 sentences around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
    @Agent(name = "creative-writer-agent", description = "Generate a story based on the given topic", outputKey = "story")
    String generateStory(@V("topic") String topic);
}
