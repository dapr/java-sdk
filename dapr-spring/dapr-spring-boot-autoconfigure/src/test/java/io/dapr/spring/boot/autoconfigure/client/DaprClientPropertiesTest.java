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

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class DaprClientPropertiesTest {


  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(EnableDaprClientProperties.class);

  @Test
  @DisplayName("Should create DaprClientProperties correctly through constructor")
  public void shouldCreateDaprClientPropertiesCorrectly() {

    DaprClientProperties properties = new DaprClientProperties(
        "http://localhost", "localhost", 3500, 50001
    );

    SoftAssertions.assertSoftly(softAssertions -> {
      softAssertions.assertThat(properties.getGrpcEndpoint()).isEqualTo("localhost");
      softAssertions.assertThat(properties.getHttpEndpoint()).isEqualTo("http://localhost");
      softAssertions.assertThat(properties.getHttpPort()).isEqualTo(3500);
      softAssertions.assertThat(properties.getGrpcPort()).isEqualTo(50001);
    });
  }

  @Test
  @DisplayName("Should create DaprClientProperties correctly through setters")
  public void shouldSetDaprClientPropertiesCorrectly() {

    DaprClientProperties properties = new DaprClientProperties();

    properties.setGrpcEndpoint("localhost");
    properties.setGrpcPort(50001);
    properties.setHttpEndpoint("http://localhost");
    properties.setHttpPort(3500);

    SoftAssertions.assertSoftly(softAssertions -> {
      softAssertions.assertThat(properties.getGrpcEndpoint()).isEqualTo("localhost");
      softAssertions.assertThat(properties.getHttpEndpoint()).isEqualTo("http://localhost");
      softAssertions.assertThat(properties.getHttpPort()).isEqualTo(3500);
      softAssertions.assertThat(properties.getGrpcPort()).isEqualTo(50001);
    });
  }

  @Test
  @DisplayName("Should map DaprClient properties correctly")
  public void shouldMapDaprClientProperties() {

    runner.withSystemProperties(
        "dapr.client.http-endpoint=http://localhost",
        "dapr.client.http-port=3500",
        "dapr.client.grpc-endpoint=localhost",
        "dapr.client.grpc-port=50001"
    ).run(context -> {
      DaprClientProperties properties = context.getBean(DaprClientProperties.class);
      SoftAssertions.assertSoftly(softAssertions -> {
        softAssertions.assertThat(properties.getGrpcEndpoint()).isEqualTo("localhost");
        softAssertions.assertThat(properties.getHttpEndpoint()).isEqualTo("http://localhost");
        softAssertions.assertThat(properties.getHttpPort()).isEqualTo(3500);
        softAssertions.assertThat(properties.getGrpcPort()).isEqualTo(50001);
      });

    });

  }

  @EnableConfigurationProperties(DaprClientProperties.class)
  static class EnableDaprClientProperties {

  }
}
