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

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.dapr.it.TestUtils.assertThrowsDaprException;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
public class GRPCStateClientIT extends AbstractStateClientIT {

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  @BeforeAll
  public static void init() throws Exception {
    daprRun = startDaprApp(GRPCStateClientIT.class.getSimpleName(), 5000);
    daprClient = new DaprClientBuilder().build();
  }

  @AfterAll
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
