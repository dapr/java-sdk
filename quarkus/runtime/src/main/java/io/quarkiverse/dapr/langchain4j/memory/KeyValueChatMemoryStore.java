package io.quarkiverse.dapr.langchain4j.memory;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;

/**
 * A {@link ChatMemoryStore} backed by Dapr's key-value state store.
 * <p>
 * Messages are serialized to JSON using {@link ChatMessageSerializer} and stored
 * under the key {@code memoryId.toString()} in the configured Dapr state store.
 */
public class KeyValueChatMemoryStore implements ChatMemoryStore {

    private final DaprClient daprClient;
    private final String stateStoreName;
    private final Function<List<ChatMessage>, String> serializer;
    private final Function<String, List<ChatMessage>> deserializer;

    public KeyValueChatMemoryStore(DaprClient daprClient, String stateStoreName) {
        this(daprClient, stateStoreName,
                ChatMessageSerializer::messagesToJson,
                ChatMessageDeserializer::messagesFromJson);
    }

    KeyValueChatMemoryStore(DaprClient daprClient, String stateStoreName,
            Function<List<ChatMessage>, String> serializer,
            Function<String, List<ChatMessage>> deserializer) {
        this.daprClient = daprClient;
        this.stateStoreName = stateStoreName;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = memoryId.toString();
        State<String> state = daprClient.getState(stateStoreName, key, String.class).block();
        if (state == null || state.getValue() == null || state.getValue().isEmpty()) {
            return Collections.emptyList();
        }
        return deserializer.apply(state.getValue());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = memoryId.toString();
        String json = serializer.apply(messages);
        daprClient.saveState(stateStoreName, key, json).block();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = memoryId.toString();
        daprClient.deleteState(stateStoreName, key).block();
    }
}