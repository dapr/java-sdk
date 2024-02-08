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
import io.dapr.utils.NetworkUtils;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
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

  private OkHttpClient okHttpClient;

  private static DaprHttp daprHttp;

  /**
   * Enable the waitForSidecar to allow the gRPC to check the http endpoint for the health check
   */
  @BeforeEach
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
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
    if (daprHttp != null) {
      daprHttp.close();
    }
    server.shutdown();
    server.awaitTermination();
  }

  @Test
  public void waitForSidecarTimeoutHealthCheck() throws Exception {
    OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);

    ManagedChannel channel = InProcessChannelBuilder.forName("waitForSidecarTimeoutHealthCheck").build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel, daprHttp);

      mockInterceptor.addRule()
              .get()
              .path("/v1.0/healthz/outbound")
              .times(6)
              .respond(404, ResponseBody.create("Not Found", MediaType.get("application/json")));

    StepVerifier.create(channelFacade.waitForChannelReady(1000))
            .expectSubscription()
            .expectError(TimeoutException.class)
            .verify(Duration.ofSeconds(20));
  }

  @Test
  public void waitForSidecarOK() {
    OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();

    DaprHttp daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);

    ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
        .usePlaintext().build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel, daprHttp);

    // added since this is doing a check against the http health check endpoint
    // for parity with dotnet
    mockInterceptor.addRule()
            .get()
            .path("/v1.0/healthz/outbound")
            .respond(204);
    
    StepVerifier.create(channelFacade.waitForChannelReady(10000))
            .expectSubscription()
            .expectComplete()
            .verify();
  }
}
