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
package io.dapr.client.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationItemTest {

  @Test
  @DisplayName("Should create ConfigurationItem correctly")
  void shouldCreateConfigurationItemCorrectly() {
    ConfigurationItem item = new ConfigurationItem("application", "java-sdk", "0.0.1", Map.of(
        "creator", "devs"
    ));

    assertThat(item.getKey()).isEqualTo("application");
    assertThat(item.getValue()).isEqualTo("java-sdk");
    assertThat(item.getVersion()).isEqualTo("0.0.1");
    assertThat(item.getMetadata()).hasSize(1);
    assertThat(item.getMetadata()).hasEntrySatisfying("creator", value -> {
      assertThat(value).isEqualTo("dev");
    });
  }

  @Test
  @DisplayName("Should create with immutable metadata")
  void shouldCreateWithImmutableMetadata() {
    ConfigurationItem item = new ConfigurationItem("application", "java-sdk", "0.0.1", Map.of(
        "creator", "devs"
    ));
    assertThatThrownBy(() -> item.getMetadata().put("language", "javascript"));
  }
}
