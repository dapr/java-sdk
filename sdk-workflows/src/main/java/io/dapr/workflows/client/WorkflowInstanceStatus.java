/*
 * Copyright 2023 The Dapr Authors
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

import java.time.Instant;

/**
 * Represents a snapshot of a workflow instance's current state, including
 * metadata.
 * @deprecated Use {@link WorkflowState} instead.
 */
@Deprecated(forRemoval = true)
public interface WorkflowInstanceStatus {

  /**
   * Gets the name of the workflow.
   *
   * @return the name of the workflow
   */
  String getName();

  /**
   * Gets the unique ID of the workflow instance.
   *
   * @return the unique ID of the workflow instance
   */
  String getInstanceId();

  /**
   * Gets the current runtime status of the workflow instance at the time this
   * object was fetched.
   *
   * @return the current runtime status of the workflow instance at the time this object was fetched
   */
  WorkflowRuntimeStatus getRuntimeStatus();

  /**
   * Gets the workflow instance's creation time in UTC.
   *
   * @return the workflow instance's creation time in UTC
   */
  Instant getCreatedAt();

  /**
   * Gets the workflow instance's last updated time in UTC.
   *
   * @return the workflow instance's last updated time in UTC
   */
  Instant getLastUpdatedAt();

  /**
   * Gets the workflow instance's serialized input, if any, as a string value.
   *
   * @return the workflow instance's serialized input or {@code null}
   */
  String getSerializedInput();

  /**
   * Gets the workflow instance's serialized output, if any, as a string value.
   *
   * @return the workflow instance's serialized output or {@code null}
   */
  String getSerializedOutput();

  /**
   * Gets the failure details, if any, for the failed workflow instance.
   *
   * <p>This method returns data only if the workflow is in the
   * {@link WorkflowFailureDetails} failureDetails,
   * and only if this instance metadata was fetched with the option to include
   * output data.
   *
   * @return the failure details of the failed workflow instance or {@code null}
   */
  @Nullable
  WorkflowFailureDetails getFailureDetails();

  /**
   * Gets a value indicating whether the workflow instance was running at the time
   * this object was fetched.
   *
   * @return {@code true} if the workflow existed and was in a running state otherwise {@code false}
   */
  boolean isRunning();

  /**
   * Gets a value indicating whether the workflow instance was completed at the
   * time this object was fetched.
   *
   * <p>A workflow instance is considered completed when its runtime status value is
   * {@link WorkflowRuntimeStatus#COMPLETED},
   * {@link WorkflowRuntimeStatus#FAILED}, or
   * {@link WorkflowRuntimeStatus#TERMINATED}.
   *
   * @return {@code true} if the workflow was in a terminal state; otherwise {@code false}
   */
  boolean isCompleted();

  /**
   * Deserializes the workflow's input into an object of the specified type.
   *
   * <p>Deserialization is performed using the DataConverter that was
   * configured on the DurableTaskClient object that created this workflow
   * metadata object.
   *
   * @param type the class associated with the type to deserialize the input data
   *             into
   * @param <T>  the type to deserialize the input data into
   * @return the deserialized input value
   * @throws IllegalStateException if the metadata was fetched without the option
   *                               to read inputs and outputs
   */
  <T> T readInputAs(Class<T> type);

  /**
   * Deserializes the workflow's output into an object of the specified type.
   *
   * <p>Deserialization is performed using the DataConverter that was
   * configured on the DurableTaskClient
   * object that created this workflow metadata object.
   *
   * @param type the class associated with the type to deserialize the output data
   *             into
   * @param <T>  the type to deserialize the output data into
   * @return the deserialized input value
   * @throws IllegalStateException if the metadata was fetched without the option
   *                               to read inputs and outputs
   */
  <T> T readOutputAs(Class<T> type);

}
