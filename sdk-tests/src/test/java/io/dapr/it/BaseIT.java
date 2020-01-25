/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.LogManager;

public abstract class BaseIT {

  private static final Collection<DaprRun> DAPR_RUNS = new ArrayList<>();

  protected static DaprRun startDaprApp(
      String successMessage, Class serviceClass, Boolean useAppPort, int maxWaitMilliseconds) throws Exception {
    return startDaprApp(successMessage, serviceClass, useAppPort, true, maxWaitMilliseconds);
  }

  protected static DaprRun startDaprApp(
      String successMessage, Class serviceClass, Boolean useAppPort, Boolean useDaprPorts, int maxWaitMilliseconds) throws Exception {
    DaprRun run = new DaprRun(
        DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
        successMessage,
        serviceClass,
        maxWaitMilliseconds);
    run.start();
    run.use();
    return run;
  }

  @BeforeClass
  public static void setup() {
    LogManager.getLogManager().reset();
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    for (DaprRun app : DAPR_RUNS) {
      app.stop();
    }
  }

}
