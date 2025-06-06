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

import io.dapr.durabletask.RetryHandler;

public class WorkflowTaskOptions {

  private final WorkflowTaskRetryPolicy retryPolicy;
  private final RetryHandler retryHandler;

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy, RetryHandler retryHandler) {
    this.retryPolicy = retryPolicy;
    this.retryHandler = retryHandler;
  }

  public WorkflowTaskOptions(WorkflowTaskRetryPolicy retryPolicy) {
    this(retryPolicy, null);
  }

  public WorkflowTaskOptions(RetryHandler retryHandler) {
    this(null, retryHandler);
  }

  public WorkflowTaskRetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  public RetryHandler getRetryHandler() {
    return retryHandler;
  }

}
