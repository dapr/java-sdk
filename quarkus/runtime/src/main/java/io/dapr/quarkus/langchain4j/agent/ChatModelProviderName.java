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

package io.dapr.quarkus.langchain4j.agent;

/**
 * Static holder for the ChatModel provider name.
 * Set at build time by the deployment processor.
 */
public final class ChatModelProviderName {

  private static volatile String name = "unknown";

  private ChatModelProviderName() {
  }

  /**
   * Sets the provider name.
   *
   * @param providerName the provider name (e.g., "dapr-conversation", "ollama")
   */
  public static void set(String providerName) {
    name = providerName;
  }

  /**
   * Returns the provider name.
   *
   * @return the provider name
   */
  public static String get() {
    return name;
  }
}
