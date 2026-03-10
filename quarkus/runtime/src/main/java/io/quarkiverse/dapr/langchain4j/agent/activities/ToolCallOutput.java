package io.quarkiverse.dapr.langchain4j.agent.activities;

/**
 * Output record returned by {@link ToolCallActivity} after a {@code @Tool}-annotated
 * method has been executed. Stored in the Dapr workflow history so callers can
 * inspect what each tool call produced.
 *
 * @param toolName  name of the {@code @Tool} method that was invoked
 * @param args      string representation of the arguments that were passed to the tool
 * @param result    string representation of the value returned by the tool method
 */
public record ToolCallOutput(String toolName, String args, String result) {
}