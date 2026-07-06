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

package io.dapr.workflows.client;

import javax.annotation.Nullable;

/**
 * Exception thrown when scheduling a new workflow with an instance ID that is already in use
 * by an active workflow instance.
 *
 * <p>The Dapr runtime only rejects duplicate instance IDs of <em>active</em> instances: scheduling
 * with the instance ID of a workflow that already reached a terminal state (completed, failed or
 * terminated) succeeds and re-runs the workflow with fresh state.
 */
public class WorkflowInstanceAlreadyExistsException extends RuntimeException {

  @Nullable
  private final String instanceId;

  /**
   * Constructor for WorkflowInstanceAlreadyExistsException.
   *
   * @param instanceId the instance ID that is already in use, or null when not known.
   * @param cause      the underlying gRPC exception returned by the sidecar.
   */
  public WorkflowInstanceAlreadyExistsException(@Nullable String instanceId, Throwable cause) {
    super(instanceId == null
        ? "an active workflow with the requested instance ID already exists"
        : String.format("an active workflow with ID '%s' already exists", instanceId), cause);
    this.instanceId = instanceId;
  }

  /**
   * Returns the workflow instance ID that is already in use.
   *
   * @return the instance ID, or null when the collision was reported for a generated ID.
   */
  @Nullable
  public String getInstanceId() {
    return instanceId;
  }
}
