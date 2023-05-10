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

package io.dapr.workflows.runtime;

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;

import java.time.Duration;

public class DaprWorkflowContextImpl implements WorkflowContext {

  private final TaskOrchestrationContext innerContext;

  /**
   * Constructor for DaprWorkflowContextImpl.
   *
   * @param context TaskOrchestrationContext
   * @throws IllegalArgumentException if context is null
   */
  public DaprWorkflowContextImpl(TaskOrchestrationContext context) throws IllegalArgumentException {
    if (context == null) {
      throw new IllegalArgumentException("Inner context cannot be null");
    } else {
      this.innerContext = context;
    }
  }

  public String getName() {
    return this.innerContext.getName();
  }

  public String getInstanceId() {
    return this.innerContext.getInstanceId();
  }

  public void complete(Object o) {
    this.innerContext.complete(o);
  }

  @Override
  public Task<Void> waitForExternalEvent(String eventName, Duration timeout) {
    return innerContext.waitForExternalEvent(eventName, timeout);
  }
}
