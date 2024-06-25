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

package io.dapr.actors.client;

import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.utils.Version;
import io.dapr.v1.DaprGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Holds a client for Dapr sidecar communication. ActorClient should be reused.
 */
public class ActorClient implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActorClient.class);

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
    this(null);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param resiliencyOptions Client resiliency options.
   */
  private ActorClient(ResiliencyOptions resiliencyOptions) {
    this(buildManagedChannel(), resiliencyOptions);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param grpcManagedChannel gRPC channel.
   * @param resiliencyOptions Client resiliency options.
   */
  private ActorClient(
      ManagedChannel grpcManagedChannel,
      ResiliencyOptions resiliencyOptions) {
    this.grpcManagedChannel = grpcManagedChannel;
    this.daprClient = buildDaprClient(grpcManagedChannel, resiliencyOptions);
  }

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param actorType   Type of actor.
   * @param actorId     Actor Identifier.
   * @param methodName  Method name to invoke.
   * @param jsonPayload Serialized body.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
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
   * @return GRPC managed channel or null.
   */
  private static ManagedChannel buildManagedChannel() {
    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throw new IllegalArgumentException("Invalid port.");
    }

    return ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), port)
      .usePlaintext()
      .userAgent(Version.getSdkVersion())
      .build();
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  private static DaprClient buildDaprClient(
      Channel grpcManagedChannel,
      ResiliencyOptions resiliencyOptions) {
    return new DaprClientImpl(DaprGrpc.newStub(grpcManagedChannel), resiliencyOptions);
  }
}
