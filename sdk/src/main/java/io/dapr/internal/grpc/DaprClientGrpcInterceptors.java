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
import io.dapr.internal.grpc.interceptors.DaprTimeoutInterceptor;
import io.dapr.internal.grpc.interceptors.DaprTracingInterceptor;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.grpc.stub.AbstractStub;
import reactor.util.context.ContextView;

/**
 * Class to be used as part of your service's client stub interceptor.
 * Usage: myClientStub = DaprClientGrpcInterceptors.intercept(myClientStub);
 */
public class DaprClientGrpcInterceptors {

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId the appId to be invoked
   * @param client gRPC client
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(final String appId, final T client) {
    return intercept(appId, client, null, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(final T client) {
    return intercept(null, client, null, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId the appId to be invoked
   * @param client gRPC client
   * @param timeoutPolicy timeout policy for gRPC call
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(
      final String appId, final T client, final TimeoutPolicy timeoutPolicy) {
    return intercept(appId, client, timeoutPolicy, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param timeoutPolicy timeout policy for gRPC call
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(final T client, final TimeoutPolicy timeoutPolicy) {
    return intercept(null, client, timeoutPolicy, null);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId the appId to be invoked
   * @param client gRPC client
   * @param context Reactor context for tracing
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(
      final String appId, final T client, final ContextView context) {
    return intercept(appId, client, null, context);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param context Reactor context for tracing
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(final T client, final ContextView context) {
    return intercept(null, client, null, context);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param client gRPC client
   * @param timeoutPolicy timeout policy for gRPC call
   * @param context Reactor context for tracing
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(
      final T client,
      final TimeoutPolicy timeoutPolicy,
      final ContextView context) {
    return intercept(null, client, timeoutPolicy, context);
  }

  /**
   * Adds all Dapr interceptors to a gRPC async stub.
   * @param appId the appId to be invoked
   * @param client gRPC client
   * @param timeoutPolicy timeout policy for gRPC call
   * @param context Reactor context for tracing
   * @param <T> async client type
   * @return async client instance with interceptors
   */
  public static <T extends AbstractStub<T>> T intercept(
      final String appId,
      final T client,
      final TimeoutPolicy timeoutPolicy,
      final ContextView context) {
    if (client == null) {
      throw new IllegalArgumentException("client cannot be null");
    }

    return client.withInterceptors(
        new DaprAppIdInterceptor(appId),
        new DaprApiTokenInterceptor(),
        new DaprTimeoutInterceptor(timeoutPolicy),
        new DaprTracingInterceptor(context));
  }

}
