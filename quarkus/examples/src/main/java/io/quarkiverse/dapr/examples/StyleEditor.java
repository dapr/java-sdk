package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sub-agent that edits a story to improve its writing style.
 */
public interface StyleEditor {

    @UserMessage("""
            You are a style editor.
            Review the following story and improve its style to match the requested style: {{style}}.
            Return only the improved story and nothing else.
            Story: {{story}}
            """)
    @Agent(name="style-editor-agent", description = "Edit a story to improve its writing style", outputKey = "story")
    String editStory(@V("story") String story, @V("style") String style);
}
