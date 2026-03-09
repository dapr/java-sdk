/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.spring.boot.properties.statestore;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class DaprStateStorePropertiesTest {


  final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(EnableDaprStateStoreProperties.class);

  @Test
  @DisplayName("Should create DaprStateStoreProperties via constructor")
  void shouldSetDaprStateStorePropertiesCorrectly() {
    DaprStateStoreProperties properties = new DaprStateStoreProperties();
    properties.setBinding("binding");
    properties.setName("name");

    SoftAssertions.assertSoftly(softAssertions -> {
      softAssertions.assertThat(properties.getName()).isEqualTo("name");
      softAssertions.assertThat(properties.getBinding()).isEqualTo("binding");
    });
  }

  @Test
  @DisplayName("Should map Dapr state store properties correctly")
  void shouldMapDaprStateStoreProperties() {
    runner.withPropertyValues(
        "dapr.statestore.name=name",
        "dapr.statestore.binding=binding"
    ).run(context -> {
      DaprStateStoreProperties properties = context.getBean(DaprStateStoreProperties.class);

      SoftAssertions.assertSoftly(softly -> {
        softly.assertThat(properties.getBinding()).isEqualTo("binding");
        softly.assertThat(properties.getName()).isEqualTo("name");
      });
    });

  }

  @EnableConfigurationProperties(DaprStateStoreProperties.class)
  static class EnableDaprStateStoreProperties {

  }

}
