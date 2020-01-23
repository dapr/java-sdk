/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.Optional;

import static io.dapr.it.DaprIntegrationTestingRunner.DAPR_FREEPORTS;

@Ignore
public class BaseIT {

  protected static DaprIntegrationTestingRunner daprIntegrationTestingRunner;


  @ClassRule
  public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @BeforeClass
  public static void setEnvironmentVariables(){
    environmentVariables.set("DAPR_HTTP_PORT", String.valueOf(DAPR_FREEPORTS.getHttpPort()));
    environmentVariables.set("DAPR_GRPC_PORT", String.valueOf(DAPR_FREEPORTS.getGrpcPort()));
  }

  public static DaprIntegrationTestingRunner createDaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean useAppPort, int sleepTime) {
    return new DaprIntegrationTestingRunner(successMessage, serviceClass, useAppPort, sleepTime);
  }

  @AfterClass
  public static void cleanUp() {
    Optional.ofNullable(daprIntegrationTestingRunner).ifPresent(daprRunner ->  daprRunner.destroyDapr());
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
