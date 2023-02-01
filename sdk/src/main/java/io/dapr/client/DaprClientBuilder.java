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

package io.dapr.client;

import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.Version;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * A builder for the DaprClient,
 * Currently only gRPC and HTTP Client will be supported.
 */
public class DaprClientBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DaprClientBuilder.class);

  /**
   * Determine if this builder will create GRPC clients instead of HTTP clients.
   */
  private final DaprApiProtocol apiProtocol;

  /**
   * Determine if this builder will use HTTP client for service method invocation APIs.
   */
  private final DaprApiProtocol methodInvocationApiProtocol;

  /**
   * Builder for Dapr's HTTP Client.
   */
  private final DaprHttpBuilder daprHttpBuilder;

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
    this.apiProtocol = Properties.API_PROTOCOL.get();
    this.methodInvocationApiProtocol = Properties.API_METHOD_INVOCATION_PROTOCOL.get();
    this.daprHttpBuilder = new DaprHttpBuilder();
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

    if (objectSerializer.getContentType() == null || objectSerializer.getContentType().isEmpty()) {
      throw new IllegalArgumentException("Content Type should not be null or empty");
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
    if (this.apiProtocol == DaprApiProtocol.HTTP) {
      LOGGER.warn("HTTP client protocol is deprecated and will be removed in Dapr's Java SDK version 1.10.");
    }

    if (this.apiProtocol != this.methodInvocationApiProtocol) {
      return new DaprClientProxy(buildDaprClient(this.apiProtocol), buildDaprClient(this.methodInvocationApiProtocol));
    }

    return buildDaprClient(this.apiProtocol);
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws IllegalStateException if any required field is missing
   */
  public DaprPreviewClient buildPreviewClient() {
    return (DaprPreviewClient) buildDaprClient(this.apiProtocol);
  }

  /**
   * Creates an instance of a Dapr Client based on the chosen protocol.
   *
   * @param protocol Dapr API's protocol.
   * @return the GRPC Client.
   * @throws java.lang.IllegalStateException if either host is missing or if port is missing or a negative number.
   */
  private DaprClient buildDaprClient(DaprApiProtocol protocol) {
    if (protocol == null) {
      throw new IllegalStateException("Protocol is required.");
    }

    switch (protocol) {
      case GRPC: return buildDaprClientGrpc();
      case HTTP: return buildDaprClientHttp();
      default: throw new IllegalStateException("Unsupported protocol: " + protocol.name());
    }
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
      throw new IllegalArgumentException("Invalid port.");
    }
    ManagedChannel channel = ManagedChannelBuilder.forAddress(
        Properties.SIDECAR_IP.get(), port).usePlaintext().userAgent(Version.getSdkVersion()).build();
    Closeable closeableChannel = () -> {
      if (channel != null && !channel.isShutdown()) {
        channel.shutdown();
      }
    };
    DaprGrpc.DaprStub asyncStub = DaprGrpc.newStub(channel);
    return new DaprClientGrpc(closeableChannel, asyncStub, this.objectSerializer, this.stateSerializer);
  }

  /**
   * Creates and instance of DaprClient over HTTP.
   *
   * @return DaprClient over HTTP.
   */
  private DaprClient buildDaprClientHttp() {
    return new DaprClientHttp(this.daprHttpBuilder.build(), this.objectSerializer, this.stateSerializer);
  }

}
