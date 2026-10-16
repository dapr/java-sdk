/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.quarkus.langchain4j.scope;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * An {@link AgenticScopeStore} backed by Dapr's key-value state store — the LangChain4j
 * equivalent of a LangGraph checkpointer.
 *
 * <p>When registered via {@link dev.langchain4j.agentic.scope.AgenticScopePersister},
 * LangChain4j's {@code AgenticScopeRegistry} persists every agentic scope (shared state,
 * conversation context, and agent invocations of a multi-agent workflow) on each update,
 * so agentic state survives process restarts and is shareable across replicas.
 *
 * <p>Scopes are serialized with LangChain4j's own {@link AgenticScopeSerializer} and stored
 * under {@code agenticscope||<agentId>||<memoryId>}. A companion index key
 * ({@code agenticscope||_index}) tracks all stored keys to support {@link #getAllKeys()}.
 *
 * <p>Note: index updates are synchronized within this instance; concurrent writers in other
 * replicas may race on the index (the scope entries themselves are never lost — only the
 * listing may briefly miss entries). Keys reconstructed from the index carry the
 * {@code memoryId} as a string.
 */
public class DaprAgenticScopeStore implements AgenticScopeStore {

  private static final Logger LOG = Logger.getLogger(DaprAgenticScopeStore.class);

  private static final String KEY_PREFIX = "agenticscope||";
  private static final String INDEX_KEY = KEY_PREFIX + "_index";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final DaprClient daprClient;
  private final String stateStoreName;

  /**
   * Creates a new DaprAgenticScopeStore.
   *
   * @param daprClient     the Dapr client
   * @param stateStoreName the state store component name
   */
  public DaprAgenticScopeStore(DaprClient daprClient, String stateStoreName) {
    this.daprClient = daprClient;
    this.stateStoreName = stateStoreName;
  }

  @Override
  public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
    try {
      String json = AgenticScopeSerializer.toJson(agenticScope);
      daprClient.saveState(stateStoreName, stateKey(key), json).block();
      updateIndex(index -> index.add(indexEntry(key)));
      LOG.debugf("Saved agentic scope %s (%d bytes)", stateKey(key), json.length());
      return true;
    } catch (Exception e) {
      LOG.errorf("Failed to save agentic scope %s: %s", stateKey(key), e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
    try {
      State<String> state = daprClient
          .getState(stateStoreName, stateKey(key), String.class).block();
      if (state == null || state.getValue() == null || state.getValue().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(AgenticScopeSerializer.fromJson(state.getValue()));
    } catch (Exception e) {
      LOG.errorf("Failed to load agentic scope %s: %s", stateKey(key), e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public boolean delete(AgenticScopeKey key) {
    try {
      boolean existed = load(key).isPresent();
      daprClient.deleteState(stateStoreName, stateKey(key)).block();
      updateIndex(index -> index.remove(indexEntry(key)));
      return existed;
    } catch (Exception e) {
      LOG.errorf("Failed to delete agentic scope %s: %s", stateKey(key), e.getMessage());
      return false;
    }
  }

  @Override
  public Set<AgenticScopeKey> getAllKeys() {
    Set<AgenticScopeKey> keys = new HashSet<>();
    for (String entry : readIndex()) {
      int separator = entry.indexOf("||");
      if (separator > 0) {
        keys.add(new AgenticScopeKey(
            entry.substring(0, separator), entry.substring(separator + 2)));
      }
    }
    return keys;
  }

  private static String stateKey(AgenticScopeKey key) {
    return KEY_PREFIX + indexEntry(key);
  }

  private static String indexEntry(AgenticScopeKey key) {
    return key.agentId() + "||" + key.memoryId();
  }

  private Set<String> readIndex() {
    try {
      State<String> state = daprClient
          .getState(stateStoreName, INDEX_KEY, String.class).block();
      if (state == null || state.getValue() == null || state.getValue().isEmpty()) {
        return new LinkedHashSet<>();
      }
      return MAPPER.readValue(state.getValue(), new TypeReference<LinkedHashSet<String>>() {
      });
    } catch (Exception e) {
      LOG.warnf("Failed to read agentic scope index: %s", e.getMessage());
      return new LinkedHashSet<>();
    }
  }

  private synchronized void updateIndex(java.util.function.Consumer<Set<String>> mutation) {
    try {
      Set<String> index = readIndex();
      mutation.accept(index);
      daprClient.saveState(stateStoreName, INDEX_KEY,
          MAPPER.writeValueAsString(index)).block();
    } catch (Exception e) {
      LOG.warnf("Failed to update agentic scope index: %s", e.getMessage());
    }
  }
}
