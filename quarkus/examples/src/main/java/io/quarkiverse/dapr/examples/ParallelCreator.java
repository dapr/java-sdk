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

import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.service.V;

/**
 * Composite agent that orchestrates {@link StoryCreator} and {@link ResearchWriter}
 * in parallel, backed by a Dapr Workflow.
 *
 * <p>Both sub-agents execute concurrently via a {@code ParallelOrchestrationWorkflow}.
 * {@link StoryCreator} is itself a {@code @SequenceAgent} that chains
 * {@link CreativeWriter} and {@link StyleEditor} — demonstrating nested composite agents.
 * Meanwhile {@link ResearchWriter} gathers facts about the country.
 */
public interface ParallelCreator {

  @ParallelAgent(name = "parallel-creator-agent",
      outputKey = "storyAndCountryResearch",
      subAgents = { StoryCreator.class, ResearchWriter.class })
  ParallelStatus create(@V("topic") String topic, @V("country") String country, @V("style") String style);

  /**
   * Produces the final output from the parallel agent results.
   *
   * @param story the generated story
   * @param summary the generated summary
   * @return the combined parallel status
   */
  @Output
  static ParallelStatus output(String story, String summary) {
    if (story == null || summary == null) {
      return new ParallelStatus("ERROR", story, summary);
    }
    return new ParallelStatus("OK", story, summary);
  }
}
