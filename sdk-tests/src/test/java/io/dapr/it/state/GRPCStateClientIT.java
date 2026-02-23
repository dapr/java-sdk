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
import io.dapr.client.domain.State;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Map;

import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
@Testcontainers
@Tag("testcontainers")
public class GRPCStateClientIT extends AbstractStateClientIT {

  private static final Network NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7")
      .withNetwork(NETWORK)
      .withNetworkAliases("mongo");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("grpcstateclientit")
      .withComponent(new Component(
          STATE_STORE_NAME,
          "state.redis",
          "v1",
          Map.of(
              "redisHost", "redis:6379",
              "redisPassword", "",
              "actorStateStore", "true")))
      .withComponent(new Component(
          QUERY_STATE_STORE,
          "state.mongodb",
          "v1",
          Map.of(
              "host", "mongo:27017",
              "databaseName", "local",
              "collectionName", "testCollection")));

  private static DaprClient daprClient;

  @BeforeAll
  public static void init() throws Exception {
    daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
    daprClient.waitForSidecar(10000).block();
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
