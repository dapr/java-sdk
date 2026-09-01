/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.workflows.internal;

import io.dapr.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds the executor the workflow runtime falls back to when the caller supplies none.
 *
 * <p>This module is compiled for Java 17, so it cannot reference
 * {@code Executors.newVirtualThreadPerTaskExecutor()} at compile time. On Java 21 and later
 * that factory is resolved reflectively; on Java 17 through 20 a cached thread pool is used,
 * which is what the workflow runtime has always used.
 *
 * <p>Virtual threads are the default on Java 21+ and can be turned off with
 * {@link Properties#WORKFLOWS_VIRTUAL_THREADS_ENABLED} — useful when activity code holds monitors
 * across blocking calls, which pins carrier threads on Java 21 through 23.
 *
 * <p>Callers that want explicit control should pass their own executor to
 * {@code WorkflowRuntimeBuilder.withExecutorService(...)} instead of relying on this default.
 * An executor supplied that way is never shut down by the runtime.
 */
public final class DefaultExecutorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutorService.class);

  private static final int VIRTUAL_THREADS_SINCE = 21;

  private DefaultExecutorService() {
  }

  /**
   * Creates the default executor for the current runtime.
   *
   * @param properties configuration used to resolve the virtual-threads opt-out.
   * @return a virtual-thread-per-task executor on Java 21+ unless virtual threads are disabled,
   *     otherwise a cached thread pool.
   */
  public static ExecutorService create(Properties properties) {
    if (!properties.getValue(Properties.WORKFLOWS_VIRTUAL_THREADS_ENABLED)) {
      LOGGER.info("Virtual threads are disabled by configuration, "
          + "using a cached thread pool for workflow and activity execution");
      return Executors.newCachedThreadPool();
    }

    if (Runtime.version().feature() >= VIRTUAL_THREADS_SINCE) {
      try {
        Method factory = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
        ExecutorService executorService = (ExecutorService) factory.invoke(null);
        LOGGER.info("Using a virtual thread per task executor for workflow and activity execution");
        return executorService;
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
        LOGGER.warn("Virtual threads are unavailable on this Java {} runtime, "
            + "falling back to a cached thread pool", Runtime.version().feature(), ex);
      }
    }

    return Executors.newCachedThreadPool();
  }
}
