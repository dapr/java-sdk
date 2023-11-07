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

import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
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
  private final DaprHttp daprHttpClient = new DaprHttpBuilder().build();


  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param channel A Managed GRPC channel
   * @see DaprClientBuilder
   */
  GrpcChannelFacade(ManagedChannel channel) {
    this.channel = channel;
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
    // Do the Dapr Http endpoint check to have parity with Dotnet
    Mono<DaprHttp.Response> responseMono = this.daprHttpClient.invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments,
            null, "", null, null);

    return responseMono
            .retryWhen(retrySpec)
            .timeout(Duration.ofMillis(timeoutInMilliseconds))
            .onErrorResume(DaprException.class, e ->
                    Mono.error(new RuntimeException(e)))
            .switchIfEmpty(DaprException.wrapMono(new RuntimeException("Health check timed out")))
            .then();
  }
}
