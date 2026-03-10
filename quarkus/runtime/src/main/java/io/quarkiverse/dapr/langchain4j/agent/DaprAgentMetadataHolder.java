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

package io.quarkiverse.dapr.langchain4j.agent;

/**
 * Thread-local holder for {@code @Agent} annotation metadata.
 *
 * <p>The generated CDI decorator sets this at the start of every {@code @Agent} method call
 * so that {@link DaprChatModelDecorator} can retrieve the real agent name, user message,
 * and system message when it lazily activates a workflow — instead of falling back to
 * {@code "standalone"} with {@code null} messages.
 */
public final class DaprAgentMetadataHolder {

  /**
   * Metadata record for agent name, user message, and system message.
   */
  public record AgentMetadata(String agentName, String userMessage, String systemMessage) {
  }

  private static final ThreadLocal<AgentMetadata> METADATA = new ThreadLocal<>();

  private DaprAgentMetadataHolder() {
  }

  /**
   * Sets the agent metadata for the current thread.
   *
   * @param agentName     the agent name
   * @param userMessage   the user message template
   * @param systemMessage the system message template
   */
  public static void set(String agentName, String userMessage, String systemMessage) {
    METADATA.set(new AgentMetadata(agentName, userMessage, systemMessage));
  }

  /**
   * Returns the agent metadata for the current thread.
   *
   * @return the agent metadata, or {@code null} if not set
   */
  public static AgentMetadata get() {
    return METADATA.get();
  }

  /**
   * Clears the agent metadata for the current thread.
   */
  public static void clear() {
    METADATA.remove();
  }
}
