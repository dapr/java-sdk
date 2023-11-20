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
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * Facade for common operations on gRPC channel.
 *
 * @see DaprGrpc
 * @see DaprClient
 */
class GrpcChannelFacade implements Closeable {

  /**
   * The GRPC managed channel to be used.
   */
  private final ManagedChannel channel;

  /**
   * The reference to the DaprHttp client.
   */
  private final DaprHttp daprHttp;


  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel A Managed GRPC channel
   * @see DaprClientBuilder
   */
  GrpcChannelFacade(ManagedChannel channel, DaprHttp daprHttp) {
    this.channel = channel;
    this.daprHttp = daprHttp;
  }

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel A Managed GRPC channel
   * @see DaprClientBuilder
   */
  GrpcChannelFacade(ManagedChannel channel) {
    this.channel = channel;
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    this.daprHttp = new DaprHttp(Properties.SIDECAR_IP.get(), 3500, okHttpClient);
  }

  @Override
  public void close() throws IOException {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  public Mono<Void> waitForChannelReady(int timeoutInMilliseconds) {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "healthz", "outbound"};
    int maxRetries = 5;

    Retry retrySpec = Retry
            .fixedDelay(maxRetries, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> {
              System.out.println("Retrying component health check...");
            });

    /*
    NOTE: (Cassie) Uncomment this once it actually gets implemented:
    https://github.com/grpc/grpc-java/issues/4359

    int maxChannelStateRetries = 5;

    // Retry logic for checking the channel state
    Retry channelStateRetrySpec = Retry
            .fixedDelay(maxChannelStateRetries, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> {
              System.out.println("Retrying channel state check...");
            });
    */

    // Do the Dapr Http endpoint check to have parity with Dotnet
    Mono<DaprHttp.Response> responseMono = this.daprHttp.invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments,
            null, "", null, null);

    return responseMono
            .retryWhen(retrySpec)
            /*
            NOTE: (Cassie) Uncomment this once it actually gets implemented:
            https://github.com/grpc/grpc-java/issues/4359
            .flatMap(response -> {
              // Check the status code
              int statusCode = response.getStatusCode();

              // Check if the channel's state is READY
              return Mono.defer(() -> {
                if (this.channel.getState(true) == ConnectivityState.READY) {
                  // Return true if the status code is in the 2xx range
                  if (statusCode >= 200 && statusCode < 300) {
                    return Mono.empty(); // Continue with the flow
                  }
                }
                return Mono.error(new RuntimeException("Health check failed"));
              }).retryWhen(channelStateRetrySpec);
            })
            */
            .timeout(Duration.ofMillis(timeoutInMilliseconds))
            .onErrorResume(DaprException.class, e ->
                    Mono.error(new RuntimeException(e)))
            .switchIfEmpty(DaprException.wrapMono(new RuntimeException("Health check timed out")))
            .then();
  }
}
