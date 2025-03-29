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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.actors.client.ActorClient;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
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
public class DaprClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(DaprConnectionDetails.class)
  DaprConnectionDetails daprConnectionDetails(DaprClientProperties properties) {
    return new PropertiesDaprConnectionDetails(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  DaprClientBuilder daprClientBuilder(DaprConnectionDetails daprConnectionDetails) {
    DaprClientBuilder builder = new DaprClientBuilder();
    if (daprConnectionDetails.httpEndpoint() != null) {
      builder.withPropertyOverride(Properties.HTTP_ENDPOINT, daprConnectionDetails.httpEndpoint());
    }
    if (daprConnectionDetails.grpcEndpoint() != null) {
      builder.withPropertyOverride(Properties.GRPC_ENDPOINT, daprConnectionDetails.grpcEndpoint());
    }
    if (daprConnectionDetails.httpPort() != null) {
      builder.withPropertyOverride(Properties.HTTP_PORT, String.valueOf(daprConnectionDetails.httpPort()));
    }
    if (daprConnectionDetails.grpcPort() != null) {
      builder.withPropertyOverride(Properties.GRPC_PORT, String.valueOf(daprConnectionDetails.grpcPort()));
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

  private Properties createPropertiesFromConnectionDetails(DaprConnectionDetails daprConnectionDetails) {
    final Map<String, String> propertyOverrides = new HashMap<>();
    propertyOverrides.put(Properties.HTTP_ENDPOINT.getName(), daprConnectionDetails.httpEndpoint());
    if(daprConnectionDetails.httpPort() != null) {
      propertyOverrides.put(Properties.HTTP_PORT.getName(), String.valueOf(daprConnectionDetails.httpPort()));
    }
    propertyOverrides.put(Properties.GRPC_ENDPOINT.getName(), daprConnectionDetails.grpcEndpoint());
    if(daprConnectionDetails.grpcPort() != null) {
      propertyOverrides.put(Properties.GRPC_PORT.getName(), String.valueOf(daprConnectionDetails.grpcPort()));
    }
    return new Properties(propertyOverrides);
  }



}
