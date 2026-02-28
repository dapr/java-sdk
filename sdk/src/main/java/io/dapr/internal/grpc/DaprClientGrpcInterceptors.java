/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.internal.grpc;

import io.dapr.internal.grpc.interceptors.DaprApiTokenInterceptor;
import io.dapr.internal.grpc.interceptors.DaprAppIdInterceptor;
import io.dapr.internal.grpc.interceptors.DaprBaggageInterceptor;
import io.dapr.internal.grpc.interceptors.DaprMetadataReceiverInterceptor;
import io.dapr.internal.grpc.interceptors.DaprTimeoutInterceptor;
import io.dapr.internal.grpc.interceptors.DaprTracingInterceptor;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

/**
 * Class to be used as part of your service's client stub interceptor.
 * Usage: myClientStub = DaprClientGrpcInterceptors.intercept(myClientStub);
 */
public class DaprClientGrpcInterceptors {

  private final String daprApiToken;

  private final TimeoutPolicy timeoutPolicy;

  /**
   * Instantiates a holder of all gRPC interceptors.
   */
  public DaprClientGrpcInterceptors() {
    this(null, null);
  }

  /**
   * Instantiates a holder of all gRPC interceptors.
   * @param daprApiToken Dapr API token.
   * @param timeoutPolicy Timeout Policy.
   */
  public DaprClientGrpcInterceptors(String daprApiToken, TimeoutPolicy timeoutPolicy) {
    this.daprApiToken = daprApiToken;
    this.timeoutPolicy = timeoutPolicy;
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public <T extends AbstractStub<T>> T intercept(final T client) {
    return intercept(null, client, null, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId Application ID to invoke.
   * @param client gRPC client
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public <T extends AbstractStub<T>> T intercept(
      final String appId,
      final T client) {
    return this.intercept(appId, client, null, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param context Reactor context for tracing
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public <T extends AbstractStub<T>> T intercept(
      final T client,
      final ContextView context) {
    return intercept(null, client, context, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param context Reactor context for tracing
   * @param metadataConsumer Consumer of the gRPC metadata
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public <T extends AbstractStub<T>> T intercept(
      final T client,
      final ContextView context,
      final Consumer<Metadata> metadataConsumer) {
    return this.intercept(null, client, context, metadataConsumer);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId Application ID to invoke.
   * @param client gRPC client
   * @param context Reactor context for tracing
   * @param metadataConsumer Consumer of the gRPC metadata
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public <T extends AbstractStub<T>> T intercept(
      final String appId,
      final T client,
      final ContextView context,
      final Consumer<Metadata> metadataConsumer) {
    if (client == null) {
      throw new IllegalArgumentException("client cannot be null");
    }

    return client.withInterceptors(
        new DaprAppIdInterceptor(appId),
        new DaprApiTokenInterceptor(this.daprApiToken),
        new DaprTimeoutInterceptor(this.timeoutPolicy),
        new DaprTracingInterceptor(context),
        new DaprBaggageInterceptor(context),
        new DaprMetadataReceiverInterceptor(metadataConsumer));
  }

}
