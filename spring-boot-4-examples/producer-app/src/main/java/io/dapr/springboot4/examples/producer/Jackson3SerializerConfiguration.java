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

package io.dapr.springboot4.examples.producer;

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.spring.boot4.autoconfigure.client.Jackson3ObjectSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Example configuration showing how to provide a custom {@link DaprObjectSerializer}
 * backed by a customized Jackson 3 {@link JsonMapper}.
 *
 * <p>By defining a {@link DaprObjectSerializer} bean here, the default auto-configured
 * serializer from {@code DaprJackson3SB4AutoConfiguration} is skipped (due to
 * {@code @ConditionalOnMissingBean}).
 *
 * <p>To use the default auto-configured serializer instead, simply remove or
 * comment out this class.
 */
@Configuration
public class Jackson3SerializerConfiguration {

  @Bean
  public DaprObjectSerializer daprObjectSerializer() {
    JsonMapper customMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .build();
    return new Jackson3ObjectSerializer(customMapper);
  }
}
