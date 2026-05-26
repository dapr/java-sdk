/*
 * Copyright 2025 The Dapr Authors
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
import io.dapr.client.domain.State;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprStateProtos;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
public class GRPCStateClientIT extends AbstractStateClientIT {

  private static DaprContainer dapr;
  private static DaprClient daprClient;

  @BeforeAll
  public static void init() {
    dapr = daprBuilder("grpc-state-it")
        .withComponent(redisStateStore(STATE_STORE_NAME))
        .withComponent(mongoStateStore(MONGO_QUERY_STATE_STORE_NAME));
    dapr.start();
    deferStop(dapr);
    daprClient = newDaprClient(dapr);
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

  /**
   * Exercises {@link DaprClient#newGrpcStub(String, java.util.function.Function)} —
   * the public API for obtaining a raw {@code DaprGrpc.DaprBlockingStub} routed
   * through the SDK's managed channel. Ports the only test from the legacy
   * {@code HelloWorldClientIT}, which previously exercised this API end-to-end
   * via {@code dapr run}.
   */
  @Test
  public void rawGrpcStubGetAndDeleteState() {
    final String key = "newGrpcStubKey";
    final String value = "Hello World";

    DaprClient daprClient = buildDaprClient();
    daprClient.saveState(STATE_STORE_NAME, key, value).block();

    DaprGrpc.DaprBlockingStub stub = daprClient.newGrpcStub("n/a", DaprGrpc::newBlockingStub);

    DaprStateProtos.GetStateResponse before = stub.getState(DaprStateProtos.GetStateRequest.newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build());
    assertEquals(value, before.getData().toStringUtf8());

    stub.deleteState(DaprStateProtos.DeleteStateRequest.newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build());

    DaprStateProtos.GetStateResponse after = stub.getState(DaprStateProtos.GetStateRequest.newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build());
    assertEquals("", after.getData().toStringUtf8());
  }

}
