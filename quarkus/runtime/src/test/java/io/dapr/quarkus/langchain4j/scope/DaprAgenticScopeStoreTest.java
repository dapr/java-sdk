package io.dapr.quarkus.langchain4j.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import reactor.core.publisher.Mono;

class DaprAgenticScopeStoreTest {

    private static final String STATE_STORE_NAME = "statestore";
    private static final String SCOPE_KEY = "agenticscope||agent-1||memory-1";
    private static final String INDEX_KEY = "agenticscope||_index";

    private DaprClient daprClient;
    private DaprAgenticScopeStore store;

    @BeforeEach
    void setUp() {
        daprClient = mock(DaprClient.class);
        store = new DaprAgenticScopeStore(daprClient, STATE_STORE_NAME);
        // Default: empty state for any key
        when(daprClient.getState(eq(STATE_STORE_NAME), anyString(), eq(String.class)))
                .thenReturn(Mono.just(new State<>("any", (String) null, (String) null)));
        when(daprClient.saveState(eq(STATE_STORE_NAME), anyString(), anyString()))
                .thenReturn(Mono.empty());
        when(daprClient.deleteState(eq(STATE_STORE_NAME), anyString()))
                .thenReturn(Mono.empty());
    }

    private static DefaultAgenticScope scope(String memoryId) {
        return AgenticScopeSerializer.fromJson(
                "{\"memoryId\":\"" + memoryId + "\",\"kind\":\"PERSISTENT\"}");
    }

    @Test
    void saveShouldSerializeScopeAndUpdateIndex() {
        boolean result = store.save(new AgenticScopeKey("agent-1", "memory-1"), scope("memory-1"));

        assertThat(result).isTrue();
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq(SCOPE_KEY), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).contains("memory-1");
        // Index updated with the new entry
        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq(INDEX_KEY),
                eq("[\"agent-1||memory-1\"]"));
    }

    @Test
    void loadShouldReturnEmptyWhenKeyDoesNotExist() {
        Optional<DefaultAgenticScope> loaded = store.load(new AgenticScopeKey("agent-1", "memory-1"));

        assertThat(loaded).isEmpty();
    }

    @Test
    void loadShouldRoundTripSavedScope() {
        String json = AgenticScopeSerializer.toJson(scope("memory-1"));
        when(daprClient.getState(eq(STATE_STORE_NAME), eq(SCOPE_KEY), eq(String.class)))
                .thenReturn(Mono.just(new State<>(SCOPE_KEY, json, (String) null)));

        Optional<DefaultAgenticScope> loaded = store.load(new AgenticScopeKey("agent-1", "memory-1"));

        assertThat(loaded).isPresent();
        assertThat(loaded.get().memoryId()).isEqualTo("memory-1");
    }

    @Test
    void deleteShouldRemoveStateAndIndexEntry() {
        String json = AgenticScopeSerializer.toJson(scope("memory-1"));
        when(daprClient.getState(eq(STATE_STORE_NAME), eq(SCOPE_KEY), eq(String.class)))
                .thenReturn(Mono.just(new State<>(SCOPE_KEY, json, (String) null)));
        when(daprClient.getState(eq(STATE_STORE_NAME), eq(INDEX_KEY), eq(String.class)))
                .thenReturn(Mono.just(new State<>(INDEX_KEY, "[\"agent-1||memory-1\"]", (String) null)));

        boolean result = store.delete(new AgenticScopeKey("agent-1", "memory-1"));

        assertThat(result).isTrue();
        verify(daprClient).deleteState(STATE_STORE_NAME, SCOPE_KEY);
        verify(daprClient).saveState(eq(STATE_STORE_NAME), eq(INDEX_KEY), eq("[]"));
    }

    @Test
    void getAllKeysShouldParseIndexEntries() {
        when(daprClient.getState(eq(STATE_STORE_NAME), eq(INDEX_KEY), eq(String.class)))
                .thenReturn(Mono.just(new State<>(INDEX_KEY,
                        "[\"agent-1||memory-1\",\"agent-2||memory-2\"]", (String) null)));

        Set<AgenticScopeKey> keys = store.getAllKeys();

        assertThat(keys).containsExactlyInAnyOrder(
                new AgenticScopeKey("agent-1", "memory-1"),
                new AgenticScopeKey("agent-2", "memory-2"));
    }

    @Test
    void getAllKeysShouldReturnEmptySetWhenIndexMissing() {
        assertThat(store.getAllKeys()).isEmpty();
    }
}
