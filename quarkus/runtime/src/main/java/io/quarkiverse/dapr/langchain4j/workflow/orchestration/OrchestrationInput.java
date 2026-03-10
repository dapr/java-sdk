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

package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input data passed to all Dapr orchestration workflows.
 *
 * @param plannerId         unique planner ID (used to look up the planner in the registry)
 * @param agentCount        number of sub-agents to execute
 * @param maxIterations     maximum loop iterations (only used by LoopOrchestrationWorkflow)
 * @param testExitAtLoopEnd whether to test exit condition at loop end vs. loop start
 */
public record OrchestrationInput(String plannerId, int agentCount, int maxIterations, boolean testExitAtLoopEnd) {
}
