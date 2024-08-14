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

package io.dapr.it.spring.messaging;

import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.core.client.DaprClientCustomizer;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.testcontainers.TestcontainersDaprClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class TestApplication {
  public static void main(String[] args) {
    SpringApplication.run(TestApplication.class, args);
  }

  @Configuration
  @EnableConfigurationProperties(DaprPubSubProperties.class)
  static class DartSpringMessagingConfiguration {

    @Bean
    public DaprClientCustomizer daprClientCustomizer(@Value("${dapr.http.port:0000}") String daprHttpPort,
                                                     @Value("${dapr.grpc.port:0000}") String daprGrpcPort){
      return new TestcontainersDaprClientCustomizer(daprHttpPort, daprGrpcPort);
    }

    @Bean
    public DaprMessagingTemplate<String> messagingTemplate(DaprClient daprClient,
                                                           DaprPubSubProperties daprPubSubProperties) {
      return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName());
    }

  }
}
