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

import com.microsoft.durabletask.TaskActivity;
import com.microsoft.durabletask.TaskActivityFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Wrapper for Durable Task Framework task activity factory.
 */
public class ActivityWrapper<T extends WorkflowActivity> implements TaskActivityFactory {
  private final Constructor<T> activityConstructor;
  private final String name;

  /**
   * Constructor for ActivityWrapper.
   *
   * @param clazz Class of the activity to wrap.
   */
  public ActivityWrapper(Class<T> clazz) {
    this.name = clazz.getCanonicalName();
    try {
      this.activityConstructor = clazz.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getName() {
    return name;
  }


  @Override
  public TaskActivity create() {
    return ctx -> {
      Object result;
      try {
        T activity = this.activityConstructor.newInstance();
        result = activity.run(new WorkflowActivityContext(ctx));
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
      return result;
    };
  }
}
