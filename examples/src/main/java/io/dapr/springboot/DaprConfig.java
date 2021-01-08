/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaprConfig {

  private static final DaprClientBuilder BUILDER = new DaprClientBuilder();

  @Bean
  public DaprClient buildDaprClient() {
    return BUILDER.build();
  }
}