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

import io.dapr.durabletask.TaskActivity;
import io.dapr.durabletask.TaskActivityFactory;
import io.dapr.workflows.WorkflowActivity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Wrapper for Durable Task Framework task activity factory.
 */
public class WorkflowActivityClassWrapper<T extends WorkflowActivity> implements TaskActivityFactory {
  private final Constructor<T> activityConstructor;
  private final String name;

  /**
   * Constructor for WorkflowActivityWrapper.
   *
   * @param name  Name of the activity to wrap.
   * @param clazz Class of the activity to wrap.
   */
  public WorkflowActivityClassWrapper(String name, Class<T> clazz) {
    this.name = name;
    try {
      this.activityConstructor = clazz.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
          String.format("No constructor found for activity class '%s'.", this.name), e);
    }
  }

  /**
   * Constructor for WorkflowActivityWrapper.
   *
   * @param clazz Class of the activity to wrap.
   */
  public WorkflowActivityClassWrapper(Class<T> clazz) {
    this(clazz.getCanonicalName(), clazz);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TaskActivity create() {
    return ctx -> {
      Object result;
      T activity;

      try {
        activity = this.activityConstructor.newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(
            String.format("Unable to instantiate instance of activity class '%s'", this.name), e);
      }

      result = activity.run(new DefaultWorkflowActivityContext(ctx));
      return result;
    };
  }
}
