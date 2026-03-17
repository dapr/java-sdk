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
 * Input for the ExitConditionCheckActivity (used by loop workflows).
 *
 * @param plannerId the planner ID to look up in the registry
 * @param iteration the current loop iteration number
 */
public record ExitConditionCheckInput(String plannerId, int iteration) {
}
