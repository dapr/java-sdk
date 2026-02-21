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
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprStateProtos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

@Testcontainers
@Tag("testcontainers")
public class HelloWorldClientIT {

  private static final String STATE_STORE_NAME = "statestore";

  private static final Network NETWORK = Network.newNetwork();

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("hello-world-state-it")
      .withComponent(new Component(
          STATE_STORE_NAME,
          "state.redis",
          "v1",
          Map.of(
              "redisHost", "redis:6379",
              "redisPassword", "")));

  @BeforeAll
  public static void waitForSidecar() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(10000).block();
      client.saveState(STATE_STORE_NAME, "mykey", "Hello World").block();
    }
  }

  @Test
  public void testHelloWorldState() throws Exception {
    try (var client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      var stub = client.newGrpcStub("n/a", DaprGrpc::newBlockingStub);

      String key = "mykey";
      {
        DaprStateProtos.GetStateRequest req = DaprStateProtos.GetStateRequest
            .newBuilder()
            .setStoreName(STATE_STORE_NAME)
            .setKey(key)
            .build();
        DaprStateProtos.GetStateResponse response = stub.getState(req);
        String value = response.getData().toStringUtf8();
        System.out.println("Got: " + value);
        Assertions.assertEquals("Hello World", value);
      }

      // Then, delete it.
      {
        DaprStateProtos.DeleteStateRequest req = DaprStateProtos.DeleteStateRequest
            .newBuilder()
            .setStoreName(STATE_STORE_NAME)
            .setKey(key)
            .build();
        stub.deleteState(req);
        System.out.println("Deleted!");
      }

      {
        DaprStateProtos.GetStateRequest req = DaprStateProtos.GetStateRequest
            .newBuilder()
            .setStoreName(STATE_STORE_NAME)
            .setKey(key)
            .build();
        DaprStateProtos.GetStateResponse response = stub.getState(req);
        String value = response.getData().toStringUtf8();
        System.out.println("Got: " + value);
        Assertions.assertEquals("", value);
      }
    }
  }
}
