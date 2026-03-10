/*
 * Copyright 2025 The Dapr Authors
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

package io.quarkiverse.dapr.examples;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

/**
 * Composite agent that orchestrates {@link CreativeWriter} and {@link StyleEditor}
 * in sequence, backed by a Dapr Workflow.
 *
 * <p>Uses {@code @SequenceAgent} which discovers the {@code DaprWorkflowAgentsBuilder}
 * via Java SPI to create the Dapr Workflow-based sequential orchestration.
 */
public interface StoryCreator {

  @SequenceAgent(name = "story-creator-agent",
      outputKey = "story",
      subAgents = { CreativeWriter.class, StyleEditor.class })
  String write(@V("topic") String topic, @V("style") String style);
}
