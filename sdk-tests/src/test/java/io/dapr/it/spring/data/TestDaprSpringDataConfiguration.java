package io.dapr.it.spring.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.spring.boot.autoconfigure.client.DaprClientAutoConfiguration;
import io.dapr.spring.data.DaprKeyValueAdapterResolver;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.spring.data.KeyValueAdapterResolver;
import io.dapr.spring.data.repository.config.EnableDaprRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableDaprRepositories
public class TestDaprSpringDataConfiguration {
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
