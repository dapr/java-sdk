package io.quarkiverse.dapr.langchain4j.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import reactor.core.publisher.Mono;

class KeyValueChatMemoryStoreTest {

    private static final String STATE_STORE_NAME = "statestore";

    private DaprClient daprClient;
    private KeyValueChatMemoryStore store;

    /**
     * Simple serializer for testing: stores the message type and text, one per line.
     * Format: "TYPE:text\nTYPE:text\n..."
     */
    private static final Function<List<ChatMessage>, String> TEST_SERIALIZER = messages -> {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            sb.append(msg.type().name()).append(":");
            switch (msg.type()) {
                case SYSTEM -> sb.append(((SystemMessage) msg).text());
                case USER -> sb.append(((UserMessage) msg).singleText());
                case AI -> sb.append(((AiMessage) msg).text());
                default -> sb.append("unknown");
            }
            sb.append("\n");
        }
        return sb.toString();
    };

    private static final Function<String, List<ChatMessage>> TEST_DESERIALIZER = json -> {
        List<ChatMessage> messages = new ArrayList<>();
        for (String line : json.split("\n")) {
            if (line.isEmpty())
                continue;
            String[] parts = line.split(":", 2);
            String type = parts[0];
            String text = parts[1];
            switch (type) {
                case "SYSTEM" -> messages.add(new SystemMessage(text));
                case "USER" -> messages.add(new UserMessage(text));
                case "AI" -> messages.add(new AiMessage(text));
            }
        }
        return messages;
    };

    @BeforeEach
    void setUp() {
        daprClient = mock(DaprClient.class);
        store = new KeyValueChatMemoryStore(daprClient, STATE_STORE_NAME,
                TEST_SERIALIZER, TEST_DESERIALIZER);
    }

    @Test
    void getMessagesShouldReturnEmptyListWhenKeyDoesNotExist() {
        State<String> emptyState = new State<>("user-1", (String) null, (String) null);
        when(daprClient.getState(eq(STATE_STORE_NAME), eq("user-1"), eq(String.class)))
                .thenReturn(Mono.just(emptyState));

        List<ChatMessage> messages = store.getMessages("user-1");

        assertThat(messages).isEmpty();
        verify(daprClient).getState(STATE_STORE_NAME, "user-1", String.class);
    }

    @Test
    void getMessagesShouldReturnEmptyListWhenValueIsEmpty() {
        State<String> emptyState = new State<>("user-1", "", (String) null);
        when(daprClient.getState(eq(STATE_STORE_NAME), eq("user-1"), eq(String.class)))
                .thenReturn(Mono.just(emptyState));

        List<ChatMessage> messages = store.getMessages("user-1");

        assertThat(messages).isEmpty();
    }

    @Test
    void updateMessagesShouldSaveSerializedMessages() {
        when(daprClient.saveState(eq(STATE_STORE_NAME), eq("user-1"), any(String.class)))
                .thenReturn(Mono.empty());

        List<ChatMessage> messages = List.of(
                new UserMessage("Hello"),
                new AiMessage("Hi there!"));

        store.updateMessages("user-1", messages);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq("user-1"), jsonCaptor.capture());

        String savedJson = jsonCaptor.getValue();
        assertThat(savedJson).contains("USER:Hello");
        assertThat(savedJson).contains("AI:Hi there!");
    }

    @Test
    void getMessagesShouldReturnDeserializedMessages() {
        String stored = "SYSTEM:You are a helpful assistant\nUSER:What is Java?\nAI:A programming language.\n";
        State<String> state = new State<>("conv-42", stored, (String) null);
        when(daprClient.getState(eq(STATE_STORE_NAME), eq("conv-42"), eq(String.class)))
                .thenReturn(Mono.just(state));

        List<ChatMessage> retrieved = store.getMessages("conv-42");

        assertThat(retrieved).hasSize(3);
        assertThat(retrieved.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) retrieved.get(0)).text()).isEqualTo("You are a helpful assistant");
        assertThat(retrieved.get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) retrieved.get(1)).singleText()).isEqualTo("What is Java?");
        assertThat(retrieved.get(2)).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) retrieved.get(2)).text()).isEqualTo("A programming language.");
    }

    @Test
    void deleteMessagesShouldRemoveState() {
        when(daprClient.deleteState(eq(STATE_STORE_NAME), eq("user-1")))
                .thenReturn(Mono.empty());

        store.deleteMessages("user-1");

        verify(daprClient).deleteState(STATE_STORE_NAME, "user-1");
    }

    @Test
    void shouldUseMemoryIdToStringAsKey() {
        when(daprClient.saveState(eq(STATE_STORE_NAME), eq("123"), any(String.class)))
                .thenReturn(Mono.empty());

        store.updateMessages(123, List.of(new UserMessage("test")));

        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq("123"), any(String.class));
    }

    @Test
    void updateMessagesShouldHandleEmptyList() {
        when(daprClient.saveState(eq(STATE_STORE_NAME), eq("user-1"), any(String.class)))
                .thenReturn(Mono.empty());

        store.updateMessages("user-1", List.of());

        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq("user-1"), any(String.class));
    }

    @Test
    void roundTripShouldPreserveMessages() {
        // Save messages
        when(daprClient.saveState(eq(STATE_STORE_NAME), eq("rt-1"), any(String.class)))
                .thenReturn(Mono.empty());

        List<ChatMessage> original = List.of(
                new SystemMessage("Be concise"),
                new UserMessage("Hello"),
                new AiMessage("Hi!"));

        store.updateMessages("rt-1", original);

        // Capture what was saved
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq("rt-1"), captor.capture());

        // Now simulate reading it back
        State<String> state = new State<>("rt-1", captor.getValue(), (String) null);
        when(daprClient.getState(eq(STATE_STORE_NAME), eq("rt-1"), eq(String.class)))
                .thenReturn(Mono.just(state));

        List<ChatMessage> retrieved = store.getMessages("rt-1");

        assertThat(retrieved).hasSize(3);
        assertThat(((SystemMessage) retrieved.get(0)).text()).isEqualTo("Be concise");
        assertThat(((UserMessage) retrieved.get(1)).singleText()).isEqualTo("Hello");
        assertThat(((AiMessage) retrieved.get(2)).text()).isEqualTo("Hi!");
    }
}
