/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.client.DaprClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(DaprClient.class)
public class DaprClientAutoConfiguration {

  /**
   * Simply creates a DaprClient instance.
   * @return returns a DaprClient factory bean
   */
  @Bean(name = "daprClientFactory")
  public DaprClientFactoryBean daprClientFactoryBean() {
    DaprClientFactoryBean factory = new DaprClientFactoryBean();
    factory.setUseGrpc(false);
    return factory;
  }
}