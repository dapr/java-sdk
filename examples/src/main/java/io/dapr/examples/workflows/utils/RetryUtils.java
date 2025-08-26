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
