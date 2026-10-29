/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.springboot.examples.eventworkflow;

import io.dapr.client.DaprClient;
import io.dapr.spring.boot.properties.pubsub.DaprPubSubProperties;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DaprPubSubProperties.class})
public class EventWorkflowConfiguration {

  public static final String ORDERS_TOPIC = "orders";
  public static final String PAYMENTS_TOPIC = "payments";
  public static final String ORDER_STATUS_TOPIC = "order-status";

  @Bean
  public DaprMessagingTemplate<Order> orderMessagingTemplate(DaprClient daprClient,
                                                             DaprPubSubProperties daprPubSubProperties) {
    return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(), false);
  }

  @Bean
  public DaprMessagingTemplate<Payment> paymentMessagingTemplate(DaprClient daprClient,
                                                                 DaprPubSubProperties daprPubSubProperties) {
    return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(), false);
  }

  @Bean
  public DaprMessagingTemplate<OrderStatus> orderStatusMessagingTemplate(DaprClient daprClient,
                                                                         DaprPubSubProperties daprPubSubProperties) {
    return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(), false);
  }
}
