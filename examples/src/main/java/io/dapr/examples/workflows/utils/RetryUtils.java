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

package io.dapr.examples.workflows.utils;

import java.time.Duration;
import java.util.concurrent.Callable;

public class RetryUtils {
  private static final long RETRY_WAIT_MILLISECONDS = 1000;

  public static String callWithRetry(Callable<String> function, Duration retryTimeout) throws InterruptedException {
    var retryTimeoutMilliseconds = retryTimeout.toMillis();
    long started = System.currentTimeMillis();
    while (true) {
      Throwable exception;
      try {
        return function.call();
      } catch (Exception | AssertionError e) {
        exception = e;
      }

      long elapsed = System.currentTimeMillis() - started;
      if (elapsed >= retryTimeoutMilliseconds) {
        if (exception instanceof RuntimeException) {
          throw (RuntimeException) exception;
        }

        throw new RuntimeException(exception);
      }

      long remaining = retryTimeoutMilliseconds - elapsed;
      Thread.sleep(Math.min(remaining, RETRY_WAIT_MILLISECONDS));
    }
  }

}
