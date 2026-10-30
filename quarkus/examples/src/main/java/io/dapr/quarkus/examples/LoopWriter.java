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

import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.service.V;

/**
 * Composite agent that runs {@link CreativeWriter} and {@link StyleEditor} in a loop
 * (2 iterations), backed by a {@code LoopOrchestrationWorkflow} Dapr Workflow.
 *
 * <p>The same sub-agents execute in every iteration, which exercises the per-iteration
 * agent-run bookkeeping (registry entries, name bindings, completion routing).
 */
public interface LoopWriter {

  @LoopAgent(name = "loop-writer-agent",
      outputKey = "story",
      maxIterations = 2,
      subAgents = { CreativeWriter.class, StyleEditor.class })
  String write(@V("topic") String topic, @V("style") String style);
}
