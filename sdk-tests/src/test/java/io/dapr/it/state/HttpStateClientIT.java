/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientHttp;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Test State HTTP DAPR capabilities using a DAPR instance with an empty service running
 */
public class HttpStateClientIT extends AbstractStateClientIT {

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(HttpStateClientIT.class.getSimpleName(), 5000);
    daprRun.switchToHTTP();
    daprClient = new DaprClientBuilder().build();
    assertTrue(daprClient instanceof DaprClientHttp);
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
