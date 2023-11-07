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

import io.dapr.config.Properties;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrpcChannelFacadeTest {

  private static int port;

  public static Server server;

  private MockInterceptor mockInterceptor;

  /**
   * Enable the waitForSidecar to allow the gRPC to check the http endpoint for the health check
   */
  @BeforeEach
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
  }

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
    VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
    StepVerifier.setDefaultTimeout(Duration.ofSeconds(20));
    int timeoutInMilliseconds = 1000;

    int unusedPort = findFreePort();
    ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", unusedPort)
        .usePlaintext().build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel);

    StepVerifier.create(channelFacade.waitForChannelReady(timeoutInMilliseconds))
            .expectSubscription()
            .then(() -> virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(timeoutInMilliseconds + timeoutInMilliseconds))) // Advance time to trigger the timeout
            .expectError(TimeoutException.class)
            .verify();
  }

  @Test
  public void waitForSidecarOK() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
        .usePlaintext().build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel);

    // added since this is doing a check against the http health check endpoint
    // for parity with dotnet
    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .respond(204);

    StepVerifier.create(channelFacade.waitForChannelReady(20000))
            .expectSubscription()
            .expectComplete()
            .verify();
  }
}
