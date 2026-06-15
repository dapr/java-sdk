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

package io.dapr.quarkus.langchain4j.chatmodel;

import dev.langchain4j.model.chat.ChatModel;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

import java.util.function.Function;

/**
 * Quarkus recorder that produces {@link DaprConversationChatModel} instances.
 */
@Recorder
public class DaprConversationRecorder {

  private static final Logger LOG = Logger.getLogger(DaprConversationRecorder.class);

  private final RuntimeValue<DaprConversationConfig> config;

  /**
   * Creates the recorder with runtime configuration.
   *
   * @param config the runtime configuration wrapped as RuntimeValue
   */
  public DaprConversationRecorder(RuntimeValue<DaprConversationConfig> config) {
    this.config = config;
  }

  /**
   * Returns a function that creates a {@link DaprConversationChatModel}.
   *
   * @return a function that creates the ChatModel from CDI context
   */
  public Function<SyntheticCreationalContext<ChatModel>, ChatModel> chatModel() {
    return ctx -> {
      DaprConversationConfig cfg = config.getValue();
      DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient();
      LOG.infof("DaprConversationChatModel created — component=%s, temperature=%.1f",
          cfg.componentName(), cfg.temperature());
      return new DaprConversationChatModel(
          client, cfg.componentName(), cfg.temperature());
    };
  }
}
