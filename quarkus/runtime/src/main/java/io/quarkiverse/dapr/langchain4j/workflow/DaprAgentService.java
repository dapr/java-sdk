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

package io.quarkiverse.dapr.langchain4j.workflow;

/**
 * Marker interface for Dapr-backed agent service implementations.
 * Provides the Dapr Workflow type name used to schedule the orchestration.
 */
public interface DaprAgentService {

  /**
   * Returns the simple class name of the Dapr Workflow to schedule
   * for this orchestration pattern.
   *
   * @return the workflow type name
   */
  String workflowType();
}
