/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.it.springboot4.testcontainers;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DaprPreviewClientConfiguration {
  @Bean
  public DaprPreviewClient daprPreviewClient(
      @Value("${dapr.http.endpoint}") String daprHttpEndpoint,
      @Value("${dapr.grpc.endpoint}") String daprGrpcEndpoint
  ){
    Map<Property<?>, String> overrides = Map.of(
            Properties.HTTP_ENDPOINT, daprHttpEndpoint,
            Properties.GRPC_ENDPOINT, daprGrpcEndpoint
    );

    return new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient();
  }
}
