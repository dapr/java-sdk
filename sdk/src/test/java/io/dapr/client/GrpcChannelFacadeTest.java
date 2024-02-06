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
import static org.mockito.ArgumentMatchers.any;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GrpcChannelFacadeTest {

  private static int port;

  public static Server server;

  private MockInterceptor mockInterceptor;

  private static OkHttpClient okHttpClient;

  private static DaprHttp daprHttp;
  private ManagedChannel mockGrpcChannel;
  /**
   * Enable the waitForSidecar to allow the gRPC to check the http endpoint for the health check
   */
  @BeforeEach
  public void setUp() {
    mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
    okHttpClient = mock(OkHttpClient.class);
    daprHttp = mock(DaprHttp.class);
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
    if (okHttpClient != null) {
      okHttpClient.dispatcher().executorService().shutdown();
      okHttpClient.connectionPool().evictAll();
      okHttpClient = null;
    }

    if (daprHttp != null) {
      daprHttp.close();
    }
    server.shutdown();
    server.awaitTermination();
  }
  private void addMockRulesForBadHealthCheck() {
    for (int i = 0; i < 6; i++) {
      mockInterceptor.addRule()
              .get()
              .path("/v1.0/healthz/outbound")
              .respond(404, ResponseBody.create("Not Found", MediaType.get("application/json")));
    }
  }
  @Test
  public void waitForSidecarTimeoutHealthCheck() throws Exception {
    VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.getOrSet();
    StepVerifier.setDefaultTimeout(Duration.ofSeconds(20));
    int timeoutInMilliseconds = 1000;

    OkHttpClient.Builder okHttpClientBuilder = mock(OkHttpClient.Builder.class);
    when(okHttpClient.newBuilder()).thenReturn(okHttpClientBuilder);
    when(okHttpClientBuilder.addInterceptor(mockInterceptor)).thenReturn(okHttpClientBuilder);

    mockGrpcChannel = mock(ManagedChannel.class);
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(mockGrpcChannel, daprHttp);

    addMockRulesForBadHealthCheck();

    when(daprHttp.invokeApi(anyString(), any(String[].class), any(), anyString(), any(), any()))
            .thenReturn(Mono.error(new TimeoutException("Simulated Timeout")));

    StepVerifier.create(channelFacade.waitForChannelReady(timeoutInMilliseconds))
            .expectSubscription()
            .then(() -> virtualTimeScheduler.advanceTimeBy(Duration.ofMillis(timeoutInMilliseconds + timeoutInMilliseconds))) // Advance time to trigger the timeout
            .expectError(TimeoutException.class)
            .verify();
  }

  @Test
  public void waitForSidecarOK() {
    OkHttpClient.Builder okHttpClientBuilder = mock(OkHttpClient.Builder.class);
    when(okHttpClient.newBuilder()).thenReturn(okHttpClientBuilder);
    when(okHttpClientBuilder.addInterceptor(mockInterceptor)).thenReturn(okHttpClientBuilder);

    mockGrpcChannel = mock(ManagedChannel.class);
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(mockGrpcChannel, daprHttp);

    byte[] responseBody = "OK".getBytes();
    Map<String, String> responseHeaders = Collections.singletonMap("Content-Type", "application/json");
    DaprHttp.Response successResponse = new DaprHttp.Response(responseBody, responseHeaders, 204);
    when(daprHttp.invokeApi(anyString(), any(String[].class), any(), anyString(), any(), any()))
            .thenReturn(Mono.just(successResponse));

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
