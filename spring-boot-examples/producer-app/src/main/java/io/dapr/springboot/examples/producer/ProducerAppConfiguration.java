package io.dapr.springboot.examples.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.pubsub.DaprPubSubProperties;
import io.dapr.spring.boot.autoconfigure.statestore.DaprStateStoreProperties;
import io.dapr.spring.data.DaprKeyValueAdapterResolver;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.spring.data.KeyValueAdapterResolver;
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
