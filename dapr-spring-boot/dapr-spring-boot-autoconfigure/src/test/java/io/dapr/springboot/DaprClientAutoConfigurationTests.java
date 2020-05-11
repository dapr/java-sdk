/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.client.DaprClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
public class DaprClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprClientAutoConfiguration.class));

  @Configuration
  static class DaprClientConfiguration {

  }

  @Test
  public void defaultServiceBacksOff() {
    this.contextRunner.withUserConfiguration(DaprClientConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(DaprClient.class);
    });
  }
}