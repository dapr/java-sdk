/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.client.DaprApiProtocol;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.config.Properties;
import io.dapr.v1.DaprGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import reactor.core.publisher.Mono;

/**
 * Holds a client for Dapr sidecar communication. ActorClient should be reused.
 */
public class ActorClient implements DaprClient, AutoCloseable {

  /**
   * gRPC channel for communication with Dapr sidecar.
   */
  private final ManagedChannel grpcManagedChannel;

  /**
   * Dapr's client.
   */
  private final DaprClient daprClient;

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   */
  public ActorClient() {
    this(Properties.API_PROTOCOL.get());
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param apiProtocol    Dapr's API protocol.
   */
  private ActorClient(DaprApiProtocol apiProtocol) {
    this(apiProtocol, buildManagedChannel(apiProtocol));
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param apiProtocol    Dapr's API protocol.
   */
  private ActorClient(DaprApiProtocol apiProtocol, ManagedChannel grpcManagedChannel) {
    this.grpcManagedChannel = grpcManagedChannel;
    this.daprClient = buildDaprClient(apiProtocol, grpcManagedChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return daprClient.invoke(actorType, actorId, methodName, jsonPayload);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    if (grpcManagedChannel != null && !grpcManagedChannel.isShutdown()) {
      grpcManagedChannel.shutdown();
    }
  }

  /**
   * Creates a GRPC managed channel (or null, if not applicable).
   *
   * @param apiProtocol Dapr's API protocol.
   * @return GRPC managed channel or null.
   */
  private static ManagedChannel buildManagedChannel(DaprApiProtocol apiProtocol) {
    if (apiProtocol != DaprApiProtocol.GRPC) {
      return null;
    }

    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throw new IllegalArgumentException("Invalid port.");
    }

    return ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), port).usePlaintext().build();
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  private static DaprClient buildDaprClient(DaprApiProtocol apiProtocol, Channel grpcManagedChannel) {
    switch (apiProtocol) {
      case GRPC: return new DaprGrpcClient(DaprGrpc.newFutureStub(grpcManagedChannel));
      case HTTP: return new DaprHttpClient(new DaprHttpBuilder().build());
      default: throw new IllegalStateException("Unsupported protocol: " + apiProtocol.name());
    }
  }
}
