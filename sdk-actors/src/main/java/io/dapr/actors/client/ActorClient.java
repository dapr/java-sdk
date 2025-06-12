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
import io.dapr.utils.NetworkUtils;
import io.dapr.utils.Version;
import io.dapr.v1.DaprGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * Holds a client for Dapr sidecar communication. ActorClient should be reused.
 */
public class ActorClient implements AutoCloseable {

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
    this(new Properties(), null);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param resiliencyOptions Client resiliency options.
   */
  public ActorClient(ResiliencyOptions resiliencyOptions) {
    this(new Properties(), resiliencyOptions);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param overrideProperties Override properties.
   */
  public ActorClient(Properties overrideProperties) {
    this(overrideProperties, null);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param overrideProperties Override properties.
   * @param resiliencyOptions Client resiliency options.
   */
  public ActorClient(Properties overrideProperties, ResiliencyOptions resiliencyOptions) {
    this(overrideProperties, null, resiliencyOptions);
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param overrideProperties Override properties.
   * @param metadata gRPC metadata or HTTP headers for actor invocation.
   * @param resiliencyOptions Client resiliency options.
   */
  public ActorClient(Properties overrideProperties, Map<String, String> metadata, ResiliencyOptions resiliencyOptions) {
    this(NetworkUtils.buildGrpcManagedChannel(overrideProperties),
        metadata,
        resiliencyOptions,
        overrideProperties.getValue(Properties.API_TOKEN));
  }

  /**
   * Instantiates a new channel for Dapr sidecar communication.
   *
   * @param grpcManagedChannel gRPC channel.
   * @param metadata gRPC metadata or HTTP headers for actor invocation.
   * @param resiliencyOptions Client resiliency options.
   * @param daprApiToken Dapr API token.
   */
  private ActorClient(
      ManagedChannel grpcManagedChannel,
      Map<String, String> metadata,
      ResiliencyOptions resiliencyOptions,
      String daprApiToken) {
    this.grpcManagedChannel = grpcManagedChannel;
    this.daprClient = buildDaprClient(grpcManagedChannel, metadata, resiliencyOptions, daprApiToken);
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
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  private static DaprClient buildDaprClient(
      Channel grpcManagedChannel,
      Map<String, String> metadata,
      ResiliencyOptions resiliencyOptions,
      String daprApiToken) {
    return new DaprClientImpl(
        DaprGrpc.newStub(grpcManagedChannel),
        metadata == null ? null : Collections.unmodifiableMap(metadata),
        resiliencyOptions,
        daprApiToken);
  }
}
