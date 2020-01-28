/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.AfterClass;

public abstract class BaseIT {

  private static final Collection<DaprRun> DAPR_RUNS = new ArrayList<>();

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, useAppPort, true, maxWaitMilliseconds);
  }

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      Boolean useDaprPorts,
      int maxWaitMilliseconds) throws Exception {
    DaprRun run = new DaprRun(
        testName,
        DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
        successMessage,
        serviceClass,
        maxWaitMilliseconds);
    DAPR_RUNS.add(run);
    run.start();
    run.use();
    return run;
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    for (DaprRun app : DAPR_RUNS) {
      app.stop();
    }
  }

}
