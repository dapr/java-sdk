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

package io.dapr.workflows;

import io.dapr.durabletask.HistoryPropagationScope;

public class WorkflowTaskOptions {

  private final WorkflowTaskRetryPolicy retryPolicy;
  private final WorkflowTaskRetryHandler retryHandler;
  private final String appId;
  private final HistoryPropagationScope historyPropagationScope;

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, WorkflowTaskRetryHandler retryHandler) {
    this(retryPolicy, retryHandler, null, null);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy) {
    this(retryPolicy, null, null, null);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryHandler retryHandler) {
    this(null, retryHandler, null, null);
  }

  /**
   * Constructor for WorkflowTaskOptions with app ID for cross-app calls.
   *
   * @param appId the ID of the app to call the activity in
   */
  public WorkflowTaskOptions(String appId) {
    this(null, null, appId, null);
  }

  /**
   * Constructor for WorkflowTaskOptions with retry policy, retry handler, and app ID.
   *
   * @param retryPolicy the retry policy
   * @param retryHandler the retry handler
   * @param appId the app ID for cross-app activity calls
   */
  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, WorkflowTaskRetryHandler retryHandler, String appId) {
    this(retryPolicy, retryHandler, appId, null);
  }

  /**
   * Constructor for WorkflowTaskOptions with all options.
   *
   * @param retryPolicy              the retry policy
   * @param retryHandler             the retry handler
   * @param appId                    the app ID for cross-app activity calls
   * @param historyPropagationScope  the history propagation scope
   */
  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, WorkflowTaskRetryHandler retryHandler,
                             String appId, HistoryPropagationScope historyPropagationScope) {
    this.retryPolicy = retryPolicy;
    this.retryHandler = retryHandler;
    this.appId = appId;
    this.historyPropagationScope = historyPropagationScope;
  }

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, String appId) {
    this(retryPolicy, null, appId, null);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryHandler retryHandler, String appId) {
    this(null, retryHandler, appId, null);
  }

  public WorkflowTaskRetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  public WorkflowTaskRetryHandler getRetryHandler() {
    return retryHandler;
  }

  public String getAppId() {
    return appId;
  }

  public HistoryPropagationScope getHistoryPropagationScope() {
    return historyPropagationScope;
  }

  /**
   * Creates a WorkflowTaskOptions configured to propagate execution history
   * with the specified scope.
   *
   * @param scope the history propagation scope
   * @return a new WorkflowTaskOptions with history propagation configured
   */
  public static WorkflowTaskOptions withHistoryPropagation(HistoryPropagationScope scope) {
    return new WorkflowTaskOptions(null, null, null, scope);
  }

  /**
   * Creates a WorkflowTaskOptions configured to propagate the caller's own history
   * plus the full ancestor chain.
   *
   * @return a new WorkflowTaskOptions with lineage propagation
   */
  public static WorkflowTaskOptions propagateLineage() {
    return withHistoryPropagation(HistoryPropagationScope.LINEAGE);
  }

  /**
   * Creates a WorkflowTaskOptions configured to propagate only the caller's own history
   * (trust boundary - ancestors are dropped).
   *
   * @return a new WorkflowTaskOptions with own-history propagation
   */
  public static WorkflowTaskOptions propagateOwnHistory() {
    return withHistoryPropagation(HistoryPropagationScope.OWN_HISTORY);
  }
}
