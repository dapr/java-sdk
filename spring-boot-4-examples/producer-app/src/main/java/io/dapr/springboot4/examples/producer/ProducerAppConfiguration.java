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

package io.dapr.springboot4.examples.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.boot.autoconfigure.statestore.DaprStateStoreProperties;
import io.dapr.spring6.data.DaprKeyValueAdapterResolver;
import io.dapr.spring6.data.DaprKeyValueTemplate;
import io.dapr.spring6.data.KeyValueAdapterResolver;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DaprPubSubProperties.class, DaprStateStoreProperties.class})
public class ProducerAppConfiguration {
  @Bean
  public ObjectMapper mapper() {
    return new ObjectMapper();
  }


  /**
   * Produce a KeyValueAdapterResolver for Dapr.
   * @param daprClient dapr client
   * @param mapper object mapper
   * @param daprStatestoreProperties properties to configure state store
   * @return KeyValueAdapterResolver
   */
  @Bean
  public KeyValueAdapterResolver keyValueAdapterResolver(DaprClient daprClient, ObjectMapper mapper,
                                                         DaprStateStoreProperties daprStatestoreProperties) {
    String storeName = daprStatestoreProperties.getName();
    String bindingName = daprStatestoreProperties.getBinding();

    return new DaprKeyValueAdapterResolver(daprClient, mapper, storeName, bindingName);
  }

  @Bean
  public DaprKeyValueTemplate daprKeyValueTemplate(KeyValueAdapterResolver keyValueAdapterResolver) {
    return new DaprKeyValueTemplate(keyValueAdapterResolver);
  }

  @Bean
  public DaprMessagingTemplate<Order> messagingTemplate(DaprClient daprClient,
                                                        DaprPubSubProperties daprPubSubProperties) {
    return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(), false);
  }

}
