/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.junit.AfterClass;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public abstract class BaseIT {

  private static final Collection<DaprRun> DAPR_RUNS = new ArrayList<>();

  protected static DaprRun startDaprApp(
    String successMessage, Class serviceClass, Boolean useAppPort, int maxWaitMilliseconds) throws Exception {
    DaprRun run = new DaprRun(DaprPorts.build(), successMessage, serviceClass, useAppPort, maxWaitMilliseconds);
    run.start();
    run.use();
    return run;
  }

  protected static void callWithRetry(Runnable function, long retryTimeoutMilliseconds) throws InterruptedException {
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

      if (System.currentTimeMillis() - started >= retryTimeoutMilliseconds) {
        throw new RuntimeException(exception);
      }
      Thread.sleep(1000);
    }
  }

  @AfterClass
  public static void cleanUp() throws IOException {
    for (DaprRun app : DAPR_RUNS) {
      app.stop();
    }
  }

}
