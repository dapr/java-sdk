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

/**
 * Sub-agent that edits a story to improve its writing style.
 */
public interface StyleEditor {

  @UserMessage("""
      You are a style editor.
      Review the following story and improve its style to match the requested style: {{style}}.
      Return only the improved story and nothing else.
      Story: {{story}}
      """)
  @Agent(name = "style-editor-agent", description = "Edit a story to improve its writing style",
      outputKey = "story")
  String editStory(@V("story") String story, @V("style") String style);
}
