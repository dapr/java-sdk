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

package io.dapr.spring.boot4.autoconfigure.client;

import io.dapr.serializer.DaprObjectSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DaprJackson3SB4AutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
          JacksonAutoConfiguration.class,
          DaprJackson3SB4AutoConfiguration.class));

  @Test
  @DisplayName("Should create DaprObjectSerializer bean when Jackson 3 is on classpath")
  void shouldCreateSerializerBean() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(DaprObjectSerializer.class);
      assertThat(context.getBean(DaprObjectSerializer.class))
          .isInstanceOf(Jackson3ObjectSerializer.class);
    });
  }

  @Test
  @DisplayName("Should use Jackson3ObjectSerializer with application/json content type")
  void shouldReturnJsonContentType() {
    contextRunner.run(context -> {
      DaprObjectSerializer serializer = context.getBean(DaprObjectSerializer.class);
      assertThat(serializer.getContentType()).isEqualTo("application/json");
    });
  }

  @Test
  @DisplayName("Should back off when user provides custom DaprObjectSerializer bean")
  void shouldBackOffWhenCustomSerializerProvided() {
    contextRunner
        .withUserConfiguration(CustomSerializerConfiguration.class)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprObjectSerializer.class);
          assertThat(context.getBean(DaprObjectSerializer.class))
              .isNotInstanceOf(Jackson3ObjectSerializer.class);
        });
  }

  @Test
  @DisplayName("Should use Spring-provided JsonMapper bean")
  void shouldUseSpringJsonMapper() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(JsonMapper.class);
      assertThat(context).hasSingleBean(DaprObjectSerializer.class);
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomSerializerConfiguration {

    @Bean
    DaprObjectSerializer customSerializer() {
      return new DaprObjectSerializer() {
        @Override
        public byte[] serialize(Object o) {
          return new byte[0];
        }

        @Override
        public <T> T deserialize(byte[] data, io.dapr.utils.TypeRef<T> type) {
          return null;
        }

        @Override
        public String getContentType() {
          return "text/plain";
        }
      };
    }
  }
}
