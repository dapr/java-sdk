/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.DaprGrpc;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.Constants;
import io.dapr.utils.Properties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import okhttp3.OkHttpClient;

/**
 * A builder for the DaprClient,
 * Currently only and HTTP Client will be supported.
 */
public class DaprClientBuilder {

  /**
   * Determine if this builder will create GRPC clients instead of HTTP clients.
   */
  private final boolean useGrpc;

  /**
   * Serializer used for request and response objects in DaprClient.
   */
  private DaprObjectSerializer objectSerializer;

  /**
   * Serializer used for state objects in DaprClient.
   */
  private DaprObjectSerializer stateSerializer;

  /**
   * Creates a constructor for DaprClient.
   *
   * {@link DefaultObjectSerializer} is used for object and state serializers by defaul but is not recommended
   * for production scenarios.
   */
  public DaprClientBuilder() {
    this.objectSerializer = new DefaultObjectSerializer();
    this.stateSerializer = new DefaultObjectSerializer();
    this.useGrpc = Properties.USE_GRPC.get();
  }

  /**
   * Sets the serializer for objects to be sent and received from Dapr.
   * See {@link DefaultObjectSerializer} as possible serializer for non-production scenarios.
   *
   * @param objectSerializer Serializer for objects to be sent and received from Dapr.
   * @return This instance.
   */
  public DaprClientBuilder withObjectSerializer(DaprObjectSerializer objectSerializer) {
    if (objectSerializer == null) {
      throw new IllegalArgumentException("Object serializer is required");
    }

    this.objectSerializer = objectSerializer;
    return this;
  }

  /**
   * Sets the serializer for objects to be persisted.
   * See {@link DefaultObjectSerializer} as possible serializer for non-production scenarios.
   *
   * @param stateSerializer Serializer for objects to be persisted.
   * @return This instance.
   */
  public DaprClientBuilder withStateSerializer(DaprObjectSerializer stateSerializer) {
    if (stateSerializer == null) {
      throw new IllegalArgumentException("State serializer is required");
    }

    this.stateSerializer = stateSerializer;
    return this;
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  public DaprClient build() {
    if (this.useGrpc) {
      return buildDaprClientGrpc();
    }

    return buildDaprClientHttp();
  }

  /**
   * Creates an instance of the GPRC Client.
   *
   * @return the GRPC Client.
   * @throws java.lang.IllegalStateException if either host is missing or if port is missing or a negative number.
   */
  private DaprClient buildDaprClientGrpc() {
    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throw new IllegalStateException("Invalid port.");
    }
    ManagedChannel channel = ManagedChannelBuilder.forAddress(Constants.DEFAULT_HOSTNAME, port).usePlaintext().build();
    return new DaprClientGrpc(DaprGrpc.newFutureStub(channel), this.objectSerializer, this.stateSerializer);
  }

  /**
   * Creates and instance of DaprClient over HTTP.
   *
   * @return DaprClient over HTTP.
   */
  private DaprClient buildDaprClientHttp() {
    int port = Properties.HTTP_PORT.get();
    if (port <= 0) {
      throw new IllegalStateException("Invalid port.");
    }
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    DaprHttp daprHttp = new DaprHttp(port, okHttpClient);
    return new DaprClientHttp(daprHttp, this.objectSerializer, this.stateSerializer);
  }
}
