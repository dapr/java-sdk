package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.spring.invoke.grpc.client.DaprGrpcBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(DaprClientAutoConfiguration.class)
public class DaprInvokeAutoConfiguration {

  @Bean
  @ConditionalOnProperty(name = "dapr.invoke.grpc.client.daprGrpcClient.enabled", havingValue = "true",
      matchIfMissing = true)
  static DaprGrpcBeanPostProcessor daprGrpcClientBeanPostProcessor(
      final ApplicationContext applicationContext) {
    return new DaprGrpcBeanPostProcessor(applicationContext);
  }
}
