package io.dapr.it.spring.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.boot.autoconfigure.client.DaprClientAutoConfiguration;
import io.dapr.spring.data.DaprKeyValueAdapterResolver;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.spring.data.KeyValueAdapterResolver;
import io.dapr.spring.data.repository.config.EnableDaprRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableDaprRepositories
@Import(DaprClientAutoConfiguration.class)
public class TestDaprSpringDataConfiguration {
  @Bean
  public ObjectMapper mapper() {
    return new ObjectMapper();
  }

  @Bean
  public KeyValueAdapterResolver keyValueAdapterResolver(DaprClient daprClient, ObjectMapper mapper) {
    String storeName = DaprSpringDataConstants.STATE_STORE_NAME;
    String bindingName = DaprSpringDataConstants.BINDING_NAME;

    return new DaprKeyValueAdapterResolver(daprClient, mapper, storeName, bindingName);
  }

  @Bean
  public DaprKeyValueTemplate daprKeyValueTemplate(KeyValueAdapterResolver keyValueAdapterResolver) {
    return new DaprKeyValueTemplate(keyValueAdapterResolver);
  }
}
