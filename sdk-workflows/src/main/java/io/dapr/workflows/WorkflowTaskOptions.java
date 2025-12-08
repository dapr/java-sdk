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

public class WorkflowTaskOptions {

  private final WorkflowTaskRetryPolicy retryPolicy;
  private final WorkflowTaskRetryHandler retryHandler;
  private final String appId;

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, WorkflowTaskRetryHandler retryHandler) {
    this(retryPolicy, retryHandler, null);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy) {
    this(retryPolicy, null, null);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryHandler retryHandler) {
    this(null, retryHandler, null);
  }

  /**
   * Constructor for WorkflowTaskOptions with app ID for cross-app calls.
   *
   * @param appId the ID of the app to call the activity in
   */
  public WorkflowTaskOptions(String appId) {
    this(null, null, appId);
  }

  /**
 * Constructor for WorkflowTaskOptions with retry policy, retry handler, and app ID.
 *
 * @param retryPolicy the retry policy
 * @param retryHandler the retry handler
 * @param appId the app ID for cross-app activity calls
 */
  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, WorkflowTaskRetryHandler retryHandler, String appId) {
    this.retryPolicy = retryPolicy;
    this.retryHandler = retryHandler;
    this.appId = appId;
  }

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, String appId) {
    this(retryPolicy, null, appId);
  }

  public WorkflowTaskOptions(WorkflowTaskRetryHandler retryHandler, String appId) {
    this(null, retryHandler, appId);
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

}
