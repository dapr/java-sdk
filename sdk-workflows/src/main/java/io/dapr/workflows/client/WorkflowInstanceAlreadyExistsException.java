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
 * Exception thrown when scheduling a new workflow with an instance ID that the Dapr runtime
 * rejects because a workflow instance with that ID already exists.
 *
 * <p>Which existing instances cause the rejection depends on
 * {@link NewWorkflowOptions#setEnforceUniqueInstanceId(boolean)}:
 *
 * <ul>
 *   <li>By default (option disabled), the runtime only rejects instance IDs that belong to an
 *   <em>active</em> instance. Scheduling with the instance ID of a workflow that already reached a
 *   terminal state (completed, failed or terminated) succeeds and re-runs the workflow with fresh
 *   state.</li>
 *   <li>When the option is enabled, the runtime rejects the instance ID if an instance with that ID
 *   exists in <em>any</em> status, including terminal ones. The existing instance is left
 *   untouched.</li>
 * </ul>
 */
public class WorkflowInstanceAlreadyExistsException extends RuntimeException {

  @Nullable
  private final String instanceId;

  /**
   * Constructor for WorkflowInstanceAlreadyExistsException.
   *
   * @param instanceId the instance ID that is already in use, or null when not known.
   * @param cause      the underlying gRPC exception returned by the sidecar. Its status description
   *                   carries the runtime's own explanation of the collision.
   */
  public WorkflowInstanceAlreadyExistsException(@Nullable String instanceId, Throwable cause) {
    super(instanceId == null
        ? "a workflow with the requested instance ID already exists"
        : String.format("a workflow with ID '%s' already exists", instanceId), cause);
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
