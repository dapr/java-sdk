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

package io.dapr.quarkus.langchain4j.durable;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import io.dapr.client.DaprClient;
import io.dapr.quarkus.langchain4j.memory.KeyValueChatMemoryStore;
import io.quarkus.arc.Arc;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat-memory load/save for durable agents, backed by {@link KeyValueChatMemoryStore} (Dapr state).
 *
 * <p><b>Load</b> runs at the proxy entry, so the prior messages are captured into the workflow
 * input and stay stable across replay. <b>Save</b> runs in the {@code memory-save} activity at the
 * end of the run and is idempotent: it <em>replaces</em> the stored conversation with the windowed
 * full history, so a redelivered (at-least-once) activity cannot duplicate messages.
 *
 * <p>The conversation is windowed to the last {@link #MAX_MESSAGES} messages. The state store name
 * comes from {@code dapr.agentic.chat-memory.store-name} (default {@code kvstore}).
 */
final class DurableChatMemory {

  static final int MAX_MESSAGES = 20;
  private static final String STORE_NAME_CONFIG = "dapr.agentic.chat-memory.store-name";

  private DurableChatMemory() {
  }

  private static KeyValueChatMemoryStore store() {
    DaprClient client = Arc.container().instance(DaprClient.class).get();
    String storeName = ConfigProvider.getConfig()
        .getOptionalValue(STORE_NAME_CONFIG, String.class).orElse("kvstore");
    return new KeyValueChatMemoryStore(client, storeName);
  }

  /**
   * Loads the windowed prior conversation as LangChain4j JSON, or {@code null} if empty.
   *
   * @param memoryId the conversation id
   * @return the serialized prior messages, or {@code null}
   */
  static String loadJson(String memoryId) {
    List<ChatMessage> windowed = window(store().getMessages(memoryId));
    return windowed.isEmpty() ? null : ChatMessageSerializer.messagesToJson(windowed);
  }

  /**
   * Replaces the stored conversation with the windowed message list (idempotent).
   *
   * @param memoryId     the conversation id
   * @param conversation the full conversation to persist
   */
  static void save(String memoryId, List<ChatMessage> conversation) {
    store().updateMessages(memoryId, window(conversation));
  }

  private static List<ChatMessage> window(List<ChatMessage> messages) {
    if (messages.size() <= MAX_MESSAGES) {
      return messages;
    }
    return new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
  }
}
