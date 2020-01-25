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
    String successMessage, Class serviceClass, Boolean useAppPort, int sleepTime) throws Exception  {
    DaprRun run = new DaprRun(DaprPorts.build(), successMessage, serviceClass, useAppPort, sleepTime);
    run.start();
    run.use();
    return run;
  }

  protected static void callWithRetry(Runnable function, long retryTimeoutMilliseconds) throws InterruptedException {
    long started = System.currentTimeMillis();
    while (true) {
      try {
        function.run();
        return;
      } catch (Exception e) {
        if (System.currentTimeMillis() - started >= retryTimeoutMilliseconds) {
          throw e;
        }
        Thread.sleep(1000);
      }
    }
  }

  @AfterClass
  public static void cleanUp() throws IOException {
    for (DaprRun app : DAPR_RUNS) {
      app.stop();
    }
  }

  public static class MyData {

    /// Gets or sets the value for PropertyA.
    private String propertyA;

    /// Gets or sets the value for PropertyB.
    private String propertyB;

    private MyData myData;

    public String getPropertyB() {
      return propertyB;
    }

    public void setPropertyB(String propertyB) {
      this.propertyB = propertyB;
    }

    public String getPropertyA() {
      return propertyA;
    }

    public void setPropertyA(String propertyA) {
      this.propertyA = propertyA;
    }

    @Override
    public String toString() {
      return "MyData{" +
        "propertyA='" + propertyA + '\'' +
        ", propertyB='" + propertyB + '\'' +
        '}';
    }

    public MyData getMyData() {
      return myData;
    }

    public void setMyData(MyData myData) {
      this.myData = myData;
    }
  }
}
