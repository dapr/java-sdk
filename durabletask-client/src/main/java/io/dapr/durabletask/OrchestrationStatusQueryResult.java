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
import java.util.List;

/**
 * Class representing the results of a filtered orchestration metadata query.
 *
 * <p>Orchestration metadata can be queried with filters using the {@link DurableTaskClient#queryInstances} method.</p>
 */
public final class OrchestrationStatusQueryResult {
  private final List<OrchestrationMetadata> orchestrationStates;
  private final String continuationToken;

  OrchestrationStatusQueryResult(List<OrchestrationMetadata> orchestrationStates, @Nullable String continuationToken) {
    this.orchestrationStates = orchestrationStates;
    this.continuationToken = continuationToken;
  }

  /**
   * Gets the list of orchestration metadata records that matched the {@link DurableTaskClient#queryInstances} query.
   *
   * @return the list of orchestration metadata records that matched the {@link DurableTaskClient#queryInstances} query.
   */
  public List<OrchestrationMetadata> getOrchestrationState() {
    return this.orchestrationStates;
  }

  /**
   * Gets the continuation token to use with the next query or {@code null} if no more metadata records are found.
   *
   * <p>Note that a non-null value does not always mean that there are more metadata records that can be returned by a
   *     query.</p>
   *
   * @return the continuation token to use with the next query or {@code null} if no more metadata records are found.
   */
  public String getContinuationToken() {
    return this.continuationToken;
  }
}
