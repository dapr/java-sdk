/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DaprClientAutoConfiguration}.
 */
class DaprClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprClientAutoConfiguration.class));

  @Test
  void daprClientBuilderConfigurer() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClientBuilderConfigurer.class));
  }

  @Test
  void daprClientBuilder() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClientBuilder.class));
  }

  @Test
  void daprClient() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClient.class));
  }

}
