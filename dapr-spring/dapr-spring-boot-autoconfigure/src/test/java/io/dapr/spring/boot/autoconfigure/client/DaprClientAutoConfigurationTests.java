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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DaprClientAutoConfiguration}.
 */
class DaprClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DaprClientAutoConfiguration.class));

  @Test
  void daprClientBuilder() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClientBuilder.class));
  }

  @Test
  void daprClient() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(DaprClient.class));
  }

  @Test
  @DisplayName("Should override properties when generating DaprClientBuilder")
  void shouldOverridePropertiesWhenGeneratingDaprClientBuilder() {
    PropertiesDaprConnectionDetails details = new PropertiesDaprConnectionDetails(
        new DaprClientProperties(
            "http://localhost", "localhost", 3500, 50001
        )
    );
    contextRunner.withBean(DaprConnectionDetails.class, () -> details).run(context -> {

      DaprClientBuilder builder = context.getBean(DaprClientBuilder.class);
      Map<String, String> propertyOverrides =
          (Map<String, String>) ReflectionTestUtils.getField(builder, "propertyOverrides");

      SoftAssertions.assertSoftly(softly -> {
        softly.assertThat(propertyOverrides.get("dapr.grpc.endpoint")).isEqualTo("localhost");
        softly.assertThat(propertyOverrides.get("dapr.http.endpoint")).isEqualTo("http://localhost");
        softly.assertThat(propertyOverrides.get("dapr.grpc.port")).isEqualTo("50001");
        softly.assertThat(propertyOverrides.get("dapr.http.port")).isEqualTo("3500");
      });
    });
  }
}
