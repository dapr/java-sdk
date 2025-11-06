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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for constructing orchestration metadata queries.
 */
public final class OrchestrationStatusQuery {
  private List<OrchestrationRuntimeStatus> runtimeStatusList = new ArrayList<>();
  private Instant createdTimeFrom;
  private Instant createdTimeTo;
  private List<String> taskHubNames = new ArrayList<>();
  private int maxInstanceCount = 100;
  private String continuationToken;
  private String instanceIdPrefix;
  private boolean fetchInputsAndOutputs;

  /**
   * Sole constructor.
   */
  public OrchestrationStatusQuery() {
  }

  /**
   * Sets the list of runtime status values to use as a filter. Only orchestration instances that have a matching
   * runtime status will be returned. The default {@code null} value will disable runtime status filtering.
   *
   * @param runtimeStatusList the list of runtime status values to use as a filter
   * @return this query object
   */
  public OrchestrationStatusQuery setRuntimeStatusList(@Nullable List<OrchestrationRuntimeStatus> runtimeStatusList) {
    this.runtimeStatusList = runtimeStatusList;
    return this;
  }

  /**
   * Include orchestration instances that were created after the specified instant.
   *
   * @param createdTimeFrom the minimum orchestration creation time to use as a filter or {@code null} to disable this
   *                        filter
   * @return this query object
   */
  public OrchestrationStatusQuery setCreatedTimeFrom(@Nullable Instant createdTimeFrom) {
    this.createdTimeFrom = createdTimeFrom;
    return this;
  }

  /**
   * Include orchestration instances that were created before the specified instant.
   *
   * @param createdTimeTo the maximum orchestration creation time to use as a filter or {@code null} to disable this
   *                      filter
   * @return this query object
   */
  public OrchestrationStatusQuery setCreatedTimeTo(@Nullable Instant createdTimeTo) {
    this.createdTimeTo = createdTimeTo;
    return this;
  }

  /**
   * Sets the maximum number of records that can be returned by the query. The default value is 100.
   *
   * <p>Requests may return fewer records than the specified page size, even if there are more records.
   * Always check the continuation token to determine whether there are more records.</p>
   *
   * @param maxInstanceCount the maximum number of orchestration metadata records to return
   * @return this query object
   */
  public OrchestrationStatusQuery setMaxInstanceCount(int maxInstanceCount) {
    this.maxInstanceCount = maxInstanceCount;
    return this;
  }

  /**
   * Include orchestration metadata records that have a matching task hub name.
   *
   * @param taskHubNames the task hub name to match or {@code null} to disable this filter
   * @return this query object
   */
  public OrchestrationStatusQuery setTaskHubNames(@Nullable List<String> taskHubNames) {
    this.taskHubNames = taskHubNames;
    return this;
  }

  /**
   * Sets the continuation token used to continue paging through orchestration metadata results.
   *
   * <p>This should always be the continuation token value from the previous query's
   * {@link OrchestrationStatusQueryResult} result.</p>
   *
   * @param continuationToken the continuation token from the previous query
   * @return this query object
   */
  public OrchestrationStatusQuery setContinuationToken(@Nullable String continuationToken) {
    this.continuationToken = continuationToken;
    return this;
  }

  /**
   * Include orchestration metadata records with the specified instance ID prefix.
   *
   * <p>For example, if there are three orchestration instances in the metadata store with IDs "Foo", "Bar", and "Baz",
   * specifying a prefix value of "B" will exclude "Foo" since its ID doesn't start with "B".</p>
   *
   * @param instanceIdPrefix the instance ID prefix filter value
   * @return this query object
   */
  public OrchestrationStatusQuery setInstanceIdPrefix(@Nullable String instanceIdPrefix) {
    this.instanceIdPrefix = instanceIdPrefix;
    return this;
  }

  /**
   * Sets whether to fetch orchestration inputs, outputs, and custom status values. The default value is {@code false}.
   *
   * @param fetchInputsAndOutputs {@code true} to fetch orchestration inputs, outputs, and custom status values,
   *                              otherwise {@code false}
   * @return this query object
   */
  public OrchestrationStatusQuery setFetchInputsAndOutputs(boolean fetchInputsAndOutputs) {
    this.fetchInputsAndOutputs = fetchInputsAndOutputs;
    return this;
  }

  /**
   * Gets the configured runtime status filter or {@code null} if none was configured.
   *
   * @return the configured runtime status filter as a list of values or {@code null} if none was configured
   */
  public List<OrchestrationRuntimeStatus> getRuntimeStatusList() {
    return runtimeStatusList;
  }

  /**
   * Gets the configured minimum orchestration creation time or {@code null} if none was configured.
   *
   * @return the configured minimum orchestration creation time or {@code null} if none was configured
   */
  @Nullable
  public Instant getCreatedTimeFrom() {
    return createdTimeFrom;
  }

  /**
   * Gets the configured maximum orchestration creation time or {@code null} if none was configured.
   *
   * @return the configured maximum orchestration creation time or {@code null} if none was configured
   */
  @Nullable
  public Instant getCreatedTimeTo() {
    return createdTimeTo;
  }

  /**
   * Gets the configured maximum number of records that can be returned by the query.
   *
   * @return the configured maximum number of records that can be returned by the query
   */
  public int getMaxInstanceCount() {
    return maxInstanceCount;
  }

  /**
   * Gets the configured task hub names to match or {@code null} if none were configured.
   *
   * @return the configured task hub names to match or {@code null} if none were configured
   */
  public List<String> getTaskHubNames() {
    return taskHubNames;
  }

  /**
   * Gets the configured continuation token value or {@code null} if none was configured.
   *
   * @return the configured continuation token value or {@code null} if none was configured
   */
  @Nullable
  public String getContinuationToken() {
    return continuationToken;
  }

  /**
   * Gets the configured instance ID prefix filter value or {@code null} if none was configured.
   *
   * @return the configured instance ID prefix filter value or {@code null} if none was configured.
   */
  @Nullable
  public String getInstanceIdPrefix() {
    return instanceIdPrefix;
  }

  /**
   * Gets the configured value that determines whether to fetch orchestration inputs, outputs, and custom status values.
   *
   * @return the configured value that determines whether to fetch orchestration inputs, outputs, and custom
   *     status values
   */
  public boolean isFetchInputsAndOutputs() {
    return fetchInputsAndOutputs;
  }
}
