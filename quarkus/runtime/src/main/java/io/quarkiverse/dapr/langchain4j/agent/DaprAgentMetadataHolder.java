package io.quarkiverse.dapr.langchain4j.agent;

/**
 * Thread-local holder for {@code @Agent} annotation metadata.
 * <p>
 * The generated CDI decorator sets this at the start of every {@code @Agent} method call
 * so that {@link DaprChatModelDecorator} can retrieve the real agent name, user message,
 * and system message when it lazily activates a workflow — instead of falling back to
 * {@code "standalone"} with {@code null} messages.
 */
public final class DaprAgentMetadataHolder {

    public record AgentMetadata(String agentName, String userMessage, String systemMessage) {
    }

    private static final ThreadLocal<AgentMetadata> METADATA = new ThreadLocal<>();

    private DaprAgentMetadataHolder() {
    }

    public static void set(String agentName, String userMessage, String systemMessage) {
        METADATA.set(new AgentMetadata(agentName, userMessage, systemMessage));
    }

    public static AgentMetadata get() {
        return METADATA.get();
    }

    public static void clear() {
        METADATA.remove();
    }
}