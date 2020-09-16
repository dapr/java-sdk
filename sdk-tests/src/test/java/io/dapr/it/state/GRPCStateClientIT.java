/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientGrpc;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
public class GRPCStateClientIT extends AbstractStateClientIT {

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(GRPCStateClientIT.class.getSimpleName(), 5000);
    daprRun.switchToGRPC();
    daprClient = new DaprClientBuilder().build();

    assertTrue(daprClient instanceof DaprClientGrpc);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    daprClient.close();
  }
  
  @Override
  protected DaprClient buildDaprClient() {
    return daprClient;
  }




}
