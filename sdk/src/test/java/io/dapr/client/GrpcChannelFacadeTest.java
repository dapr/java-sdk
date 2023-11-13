/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.client;

import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrpcChannelFacadeTest {

  private static int port;

  public static Server server;

  @BeforeAll
  public static void setup() throws IOException {
    port = findFreePort();
    server = ServerBuilder.forPort(port)
        .addService(new DaprGrpc.DaprImplBase() {
        })
        .build();
    server.start();
  }

  @AfterAll
  public static void teardown() throws InterruptedException {
    server.shutdown();
    server.awaitTermination();
  }

  @Test
  public void waitForSidecarTimeout() throws Exception {
    int unusedPort = findFreePort();
    ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", unusedPort)
        .usePlaintext().build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel);

    assertThrows(RuntimeException.class, () -> channelFacade.waitForChannelReady(1).block());
  }

  @Test
  public void waitForSidecarOK() {
      ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
          .usePlaintext().build();
      final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel);
      channelFacade.waitForChannelReady(10000).block();
  }

}
