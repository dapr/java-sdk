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

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * Sub-agent that generates a creative story draft based on a given topic.
 */
public interface CreativeWriter {

  @UserMessage("""
      You are a creative writer.
      Generate a draft of a story no more than 3 sentences around the given topic.
      Return only the story and nothing else.
      The topic is {{topic}}.
      """)
  @Agent(name = "creative-writer-agent",
      description = "Generate a story based on the given topic", outputKey = "story")
  String generateStory(@V("topic") String topic);
}
