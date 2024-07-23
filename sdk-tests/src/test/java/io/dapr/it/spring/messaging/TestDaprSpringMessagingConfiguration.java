package io.dapr.it.spring.messaging;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DaprPubSubProperties.class)
public class TestDaprSpringMessagingConfiguration {
  @Value("${dapr.client.grpc.port}")
  private int grpcPort;

  @Value("${dapr.client.http.port}")
  private int httpPort;

  @Bean
  public DaprClient daprClient() {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.GRPC_PORT, String.valueOf(grpcPort))
        .withPropertyOverride(Properties.HTTP_PORT, String.valueOf(httpPort))
        .build();
  }

  @Bean
  public DaprMessagingTemplate<String> messagingTemplate(DaprClient daprClient,
                                                         DaprPubSubProperties daprPubSubProperties) {
    return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName());
  }

}
