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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Runtime configuration for the Dapr Conversation ChatModel provider.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.langchain4j.dapr")
public interface DaprConversationConfig {

  /**
   * The Dapr conversation component name.
   *
   * @return the component name
   */
  @WithDefault("llm")
  String componentName();

  /**
   * The temperature for generation (0.0 - 2.0).
   *
   * @return the temperature
   */
  @WithDefault("0.7")
  double temperature();

  /**
   * Whether the Dapr conversation integration is enabled.
   *
   * @return true if enabled
   */
  @WithDefault("true")
  boolean enabled();
}
