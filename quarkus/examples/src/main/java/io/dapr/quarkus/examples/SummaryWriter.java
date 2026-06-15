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

package io.dapr.quarkus.examples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sub-agent that writes a one-sentence summary of a topic. Used as the second branch of
 * {@link StoryRouter} so both branches consume the same {@code topic} input.
 */
public interface SummaryWriter {

  @UserMessage("Summarize the topic {{topic}} in a single sentence. Return only the sentence.")
  @Agent(name = "summary-writer-agent", description = "Summarize a topic", outputKey = "story")
  String summarize(@V("topic") String topic);
}
