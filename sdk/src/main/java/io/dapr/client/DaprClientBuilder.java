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

import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.NetworkUtils;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * A builder for the DaprClient,
 * Currently only gRPC and HTTP Client will be supported.
 */
public class DaprClientBuilder {

  private final Map<String, String> propertyOverrides = new HashMap<>();

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
   * Resiliency configuration for DaprClient.
   */
  private ResiliencyOptions resiliencyOptions;

  /**
   * Creates a constructor for DaprClient.
   *
   * {@link DefaultObjectSerializer} is used for object and state serializers by default but is not recommended
   * for production scenarios.
   */
  public DaprClientBuilder() {
    this.objectSerializer = new DefaultObjectSerializer();
    this.stateSerializer = new DefaultObjectSerializer();
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
   * Sets the resiliency options for DaprClient.
   *
   * @param options Serializer for objects to be persisted.
   * @return This instance.
   */
  public DaprClientBuilder withResiliencyOptions(ResiliencyOptions options) {
    this.resiliencyOptions = options;
    return this;
  }

  public DaprClientBuilder withPropertyOverride(Property<?> property, String value) {
    this.propertyOverrides.put(property.getName(), value);
    return this;
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  public DaprClient build() {
    return buildDaprClient();
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws IllegalStateException if any required field is missing
   */
  public DaprPreviewClient buildPreviewClient() {
    return buildDaprClient();
  }

  /**
   * Creates an instance of the GPRC Client.
   *
   * @return the GRPC Client.
   * @throws java.lang.IllegalStateException if either host is missing or if port is missing or a negative number.
   */
  private DaprClientImpl buildDaprClient() {
    final Properties properties = new Properties(this.propertyOverrides);
    final ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel(properties);
    final DaprHttp daprHttp = this.daprHttpBuilder.build();
    final GrpcChannelFacade channelFacade = new GrpcChannelFacade(channel);
    DaprGrpc.DaprStub asyncStub = DaprGrpc.newStub(channel);
    return new DaprClientImpl(
        channelFacade,
        asyncStub,
        daprHttp,
        this.objectSerializer,
        this.stateSerializer,
        this.resiliencyOptions);
  }
}
