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

package io.dapr.durabletask;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for constructing orchestration instance purge selection criteria.
 */
public final class PurgeInstanceCriteria {

  private Instant createdTimeFrom;
  private Instant createdTimeTo;
  private List<OrchestrationRuntimeStatus> runtimeStatusList = new ArrayList<>();
  private Duration timeout;

  /**
   * Creates a new, default instance of the {@code PurgeInstanceCriteria} class.
   */
  public PurgeInstanceCriteria() {
  }

  /**
   * Purge orchestration instances that were created after the specified instant.
   *
   * @param createdTimeFrom the minimum orchestration creation time to use as a selection criteria or {@code null} to
   *                        disable this selection criteria
   * @return this criteria object
   */
  public PurgeInstanceCriteria setCreatedTimeFrom(Instant createdTimeFrom) {
    this.createdTimeFrom = createdTimeFrom;
    return this;
  }

  /**
   * Purge orchestration instances that were created before the specified instant.
   *
   * @param createdTimeTo the maximum orchestration creation time to use as a selection criteria or {@code null} to
   *                      disable this selection criteria
   * @return this criteria object
   */
  public PurgeInstanceCriteria setCreatedTimeTo(Instant createdTimeTo) {
    this.createdTimeTo = createdTimeTo;
    return this;
  }

  /**
   * Sets the list of runtime status values to use as a selection criteria. Only orchestration instances that have a
   * matching runtime status will be purged. An empty list is the same as selecting for all runtime status values.
   *
   * @param runtimeStatusList the list of runtime status values to use as a selection criteria
   * @return this criteria object
   */
  public PurgeInstanceCriteria setRuntimeStatusList(List<OrchestrationRuntimeStatus> runtimeStatusList) {
    this.runtimeStatusList = runtimeStatusList;
    return this;
  }

  /**
   * Sets a timeout duration for the purge operation. Setting to {@code null} will reset the timeout
   * to be the default value.
   *
   * @param timeout the amount of time to wait for the purge instance operation to complete
   * @return this criteria object
   */
  public PurgeInstanceCriteria setTimeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Gets the configured minimum orchestration creation time or {@code null} if none was configured.
   *
   * @return the configured minimum orchestration creation time or {@code null} if none was configured
   */
  @Nullable
  public Instant getCreatedTimeFrom() {
    return this.createdTimeFrom;
  }

  /**
   * Gets the configured maximum orchestration creation time or {@code null} if none was configured.
   *
   * @return the configured maximum orchestration creation time or {@code null} if none was configured
   */
  @Nullable
  public Instant getCreatedTimeTo() {
    return this.createdTimeTo;
  }

  /**
   * Gets the configured runtime status selection criteria.
   *
   * @return the configured runtime status filter as a list of values
   */
  public List<OrchestrationRuntimeStatus> getRuntimeStatusList() {
    return this.runtimeStatusList;
  }

  /**
   * Gets the configured timeout duration or {@code null} if none was configured.
   *
   * @return the configured timeout
   */
  @Nullable
  public Duration getTimeout() {
    return this.timeout;
  }

}
