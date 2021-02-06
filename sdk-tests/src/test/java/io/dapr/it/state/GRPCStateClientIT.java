/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static io.dapr.it.TestUtils.assertThrowsDaprException;

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
  }

  @AfterClass
  public static void tearDown() throws Exception {
    daprClient.close();
  }
  
  @Override
  protected DaprClient buildDaprClient() {
    return daprClient;
  }

  /** Tests where HTTP and GRPC behavior differ in Dapr runtime. **/

  @Test
  public void getStateStoreNotFound() {
    final String stateKey = "key";

    DaprClient daprClient = buildDaprClient();

    // DaprException is guaranteed in the Dapr SDK but getCause() is null in HTTP while present in GRPC implementation.
    assertThrowsDaprException(
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: state store unknown state store is not found",
        () -> daprClient.getState("unknown state store", new State(stateKey), byte[].class).block());
  }

  @Test
  public void getStatesStoreNotFound() {
    final String stateKey = "key";

    DaprClient daprClient = buildDaprClient();

    // DaprException is guaranteed in the Dapr SDK but getCause() is null in HTTP while present in GRPC implementation.
    assertThrowsDaprException(
        "INVALID_ARGUMENT",
        "INVALID_ARGUMENT: state store unknown state store is not found",
        () -> daprClient.getBulkState(
            "unknown state store",
            Collections.singletonList(stateKey),
            byte[].class).block());
  }

}
