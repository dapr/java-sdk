/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.utils;

class Retry {

  private static final long RETRY_WAIT_MILLISECONDS = 1000;

  private Retry() {
  }

  static void callWithRetry(Runnable function, long retryTimeoutMilliseconds) throws InterruptedException {
    long started = System.currentTimeMillis();
    while (true) {
      Throwable exception;
      try {
        function.run();
        return;
      } catch (Exception e) {
        exception = e;
      } catch (AssertionError e) {
        exception = e;
      }

      long elapsed = System.currentTimeMillis() - started;
      if (elapsed >= retryTimeoutMilliseconds) {
        if (exception instanceof RuntimeException) {
          throw (RuntimeException)exception;
        }

        throw new RuntimeException(exception);
      }

      long remaining = retryTimeoutMilliseconds - elapsed;
      Thread.sleep(Math.min(remaining, RETRY_WAIT_MILLISECONDS));
    }
  }
}
