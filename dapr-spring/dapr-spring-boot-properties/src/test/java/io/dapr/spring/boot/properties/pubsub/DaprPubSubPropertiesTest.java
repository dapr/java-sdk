/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.boot.properties.pubsub;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class DaprPubSubPropertiesTest {

  final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(EnableDaprPubSubProperties.class);


  @Test
  @DisplayName("Should configure properties with setters")
  void shouldSetProperties() {
    DaprPubSubProperties properties = new DaprPubSubProperties();
    properties.setName("pubsub");
    properties.setObservationEnabled(false);

    SoftAssertions.assertSoftly(softAssertions -> {
      softAssertions.assertThat(properties.getName()).isEqualTo("pubsub");
      softAssertions.assertThat(properties.isObservationEnabled()).isEqualTo(false);
    });
  }

  @Test
  @DisplayName("Should map DaprPubSubProperties correctly")
  void shouldMapDaprPubSubPropertiesCorrectly() {
    runner.withPropertyValues(
        "dapr.pubsub.name=pubsub",
        "dapr.pubsub.observation-enabled=true"
    ).run(context -> {
      DaprPubSubProperties properties = context.getBean(DaprPubSubProperties.class);

      SoftAssertions.assertSoftly(softAssertions -> {
        softAssertions.assertThat(properties.getName()).isEqualTo("pubsub");
        softAssertions.assertThat(properties.isObservationEnabled()).isEqualTo(true);
      });
    });
  }

  @EnableConfigurationProperties(DaprPubSubProperties.class)
  static class EnableDaprPubSubProperties {
  }
}
