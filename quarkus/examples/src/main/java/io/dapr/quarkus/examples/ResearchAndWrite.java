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

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.service.V;

/**
 * Parallel composite with an {@code @Output} combiner: runs {@link CreativeWriter} (writes
 * {@code story}) and {@link ResearchWriter} (writes {@code summary}) concurrently, then combines
 * their scope outputs into the result.
 *
 * <p>This exercises the {@code @Output} path: the {@code durable-parallel} workflow runs both
 * react-agent children, then invokes {@link #combine} to produce the result.
 */
public interface ResearchAndWrite {

  @ParallelAgent(name = "research-and-write-agent", outputKey = "combined",
      subAgents = { CreativeWriter.class, ResearchWriter.class })
  String run(@V("topic") String topic, @V("country") String country);

  /**
   * Combines the two sub-agent outputs (matched from scope by parameter name) into the result.
   *
   * @param story   the creative-writer output (scope key {@code story})
   * @param summary the research output (scope key {@code summary})
   * @return the combined text
   */
  @Output
  static String combine(String story, String summary) {
    return "STORY: " + story + "\nRESEARCH: " + summary;
  }
}
