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

package io.dapr.spring.boot4.autoconfigure.client;

import io.dapr.actors.client.ActorClient;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.spring.boot.properties.client.ClientPropertiesDaprConnectionDetails;
import io.dapr.spring.boot.properties.client.DaprClientProperties;
import io.dapr.spring.boot.properties.client.DaprConnectionDetails;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(DaprClient.class)
@EnableConfigurationProperties(DaprClientProperties.class)
public class DaprClientSB4AutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(DaprConnectionDetails.class)
  DaprConnectionDetails daprConnectionDetails(DaprClientProperties properties) {
    return new ClientPropertiesDaprConnectionDetails(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  DaprClientBuilder daprClientBuilder(DaprConnectionDetails daprConnectionDetails,
                                      ObjectProvider<DaprObjectSerializer> serializerProvider) {
    DaprClientBuilder builder = createDaprClientBuilder();

    DaprObjectSerializer serializer = serializerProvider.getIfAvailable();
    if (serializer != null) {
      builder.withObjectSerializer(serializer);
      builder.withStateSerializer(serializer);
    }

    String httpEndpoint = daprConnectionDetails.getHttpEndpoint();

    if (httpEndpoint != null) {
      builder.withPropertyOverride(Properties.HTTP_ENDPOINT, httpEndpoint);
    }

    String grpcEndpoint = daprConnectionDetails.getGrpcEndpoint();

    if (grpcEndpoint != null) {
      builder.withPropertyOverride(Properties.GRPC_ENDPOINT, grpcEndpoint);
    }

    Integer httpPort = daprConnectionDetails.getHttpPort();

    if (httpPort != null) {
      builder.withPropertyOverride(Properties.HTTP_PORT, String.valueOf(httpPort));
    }

    Integer grpcPort = daprConnectionDetails.getGrpcPort();

    if (grpcPort != null) {
      builder.withPropertyOverride(Properties.GRPC_PORT, String.valueOf(grpcPort));
    }

    String apiToken = daprConnectionDetails.getApiToken();
    if (apiToken != null) {
      builder.withPropertyOverride(Properties.API_TOKEN, apiToken);
    }

    return builder;
  }

  @Bean
  @ConditionalOnMissingBean
  DaprClient daprClient(DaprClientBuilder daprClientBuilder) {
    return daprClientBuilder.build();
  }

  @Bean
  @ConditionalOnMissingBean
  DaprWorkflowClient daprWorkflowClient(DaprConnectionDetails daprConnectionDetails) {
    Properties properties = createPropertiesFromConnectionDetails(daprConnectionDetails);
    return new DaprWorkflowClient(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  ActorClient daprActorClient(DaprConnectionDetails daprConnectionDetails) {
    Properties properties = createPropertiesFromConnectionDetails(daprConnectionDetails);
    return new ActorClient(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  ActorRuntime daprActorRuntime(DaprConnectionDetails daprConnectionDetails) {
    Properties properties = createPropertiesFromConnectionDetails(daprConnectionDetails);
    return ActorRuntime.getInstance(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  WorkflowRuntimeBuilder daprWorkflowRuntimeBuilder(DaprConnectionDetails daprConnectionDetails) {
    Properties properties = createPropertiesFromConnectionDetails(daprConnectionDetails);

    return new WorkflowRuntimeBuilder(properties);
  }

  /**
   * We use this method in tests to override the default DaprClientBuilder.
   */
  protected DaprClientBuilder createDaprClientBuilder() {
    return new DaprClientBuilder();
  }

  /**
   * Creates a Properties object from the DaprConnectionDetails.
   *
   * @param daprConnectionDetails the DaprConnectionDetails
   * @return the Properties object
   */
  protected Properties createPropertiesFromConnectionDetails(DaprConnectionDetails daprConnectionDetails) {
    Map<String, String> propertyOverrides = new HashMap<>();
    String httpEndpoint = daprConnectionDetails.getHttpEndpoint();

    if (httpEndpoint != null) {
      propertyOverrides.put(Properties.HTTP_ENDPOINT.getName(), httpEndpoint);
    }

    Integer httpPort = daprConnectionDetails.getHttpPort();

    if (httpPort != null) {
      propertyOverrides.put(Properties.HTTP_PORT.getName(), String.valueOf(httpPort));
    }

    String grpcEndpoint = daprConnectionDetails.getGrpcEndpoint();

    if (grpcEndpoint != null) {
      propertyOverrides.put(Properties.GRPC_ENDPOINT.getName(), grpcEndpoint);
    }

    Integer grpcPort = daprConnectionDetails.getGrpcPort();

    if (grpcPort != null) {
      propertyOverrides.put(Properties.GRPC_PORT.getName(), String.valueOf(grpcPort));
    }

    String apiToken = daprConnectionDetails.getApiToken();
    if (apiToken != null) {
      propertyOverrides.put(Properties.API_TOKEN.getName(), apiToken);
    }

    return new Properties(propertyOverrides);
  }

}
