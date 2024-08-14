package io.dapr.it.spring.messaging;

import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.core.client.DaprClientCustomizer;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.testcontainers.TestcontainersDaprClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DaprPubSubProperties.class)
public class TestDaprSpringMessagingConfiguration {
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
