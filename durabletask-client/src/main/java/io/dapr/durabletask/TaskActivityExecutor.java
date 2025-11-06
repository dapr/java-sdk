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

import java.util.HashMap;
import java.util.logging.Logger;

final class TaskActivityExecutor {
  private final HashMap<String, TaskActivityFactory> activityFactories;
  private final DataConverter dataConverter;
  private final Logger logger;

  public TaskActivityExecutor(
      HashMap<String, TaskActivityFactory> activityFactories,
      DataConverter dataConverter,
      Logger logger) {
    this.activityFactories = activityFactories;
    this.dataConverter = dataConverter;
    this.logger = logger;
  }

  public String execute(String taskName, String input, String taskExecutionId, int taskId) throws Throwable {
    TaskActivityFactory factory = this.activityFactories.get(taskName);
    if (factory == null) {
      throw new IllegalStateException(
          String.format("No activity task named '%s' is registered.", taskName));
    }

    TaskActivity activity = factory.create();
    if (activity == null) {
      throw new IllegalStateException(
          String.format("The task factory '%s' returned a null TaskActivity object.", taskName));
    }

    TaskActivityContextImpl context = new TaskActivityContextImpl(taskName, input, taskExecutionId, taskId);

    // Unhandled exceptions are allowed to escape
    Object output = activity.run(context);
    if (output != null) {
      return this.dataConverter.serialize(output);
    }

    return null;
  }

  private class TaskActivityContextImpl implements TaskActivityContext {
    private final String name;
    private final String rawInput;
    private final String taskExecutionId;
    private final int taskId;

    private final DataConverter dataConverter = TaskActivityExecutor.this.dataConverter;

    public TaskActivityContextImpl(String activityName, String rawInput, String taskExecutionId, int taskId) {
      this.name = activityName;
      this.rawInput = rawInput;
      this.taskExecutionId = taskExecutionId;
      this.taskId = taskId;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public <T> T getInput(Class<T> targetType) {
      if (this.rawInput == null) {
        return null;
      }

      return this.dataConverter.deserialize(this.rawInput, targetType);
    }

    @Override
    public String getTaskExecutionId() {
      return this.taskExecutionId;
    }

    @Override
    public int getTaskId() {
      return this.taskId;
    }
  }
}
