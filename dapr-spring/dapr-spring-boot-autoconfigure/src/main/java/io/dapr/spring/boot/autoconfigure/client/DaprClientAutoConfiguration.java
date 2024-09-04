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

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.spring.core.client.DaprClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.stream.Collectors;

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
    builder.withPropertyOverride(Properties.HTTP_ENDPOINT, daprConnectionDetails.httpEndpoint());
    builder.withPropertyOverride(Properties.GRPC_ENDPOINT, daprConnectionDetails.grpcEndpoint());
    builder.withPropertyOverride(Properties.HTTP_PORT, String.valueOf(daprConnectionDetails.httpPort()));
    builder.withPropertyOverride(Properties.GRPC_PORT, String.valueOf(daprConnectionDetails.grpcPort()));
    return builder;
  }

  @Bean
  @ConditionalOnMissingBean
  DaprClient daprClient(DaprClientBuilder daprClientBuilder) {
    return daprClientBuilder.build();
  }

}
