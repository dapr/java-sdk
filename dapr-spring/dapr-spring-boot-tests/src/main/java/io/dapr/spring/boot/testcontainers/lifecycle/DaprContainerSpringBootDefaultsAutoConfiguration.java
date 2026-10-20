/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.spring.boot.testcontainers.lifecycle;

import io.dapr.testcontainers.DaprContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link DaprContainerSpringBootDefaultsPostProcessor}. It can be disabled by setting
 * {@code dapr.testcontainers.app-defaults.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(DaprContainer.class)
@ConditionalOnProperty(prefix = "dapr.testcontainers.app-defaults", name = "enabled", matchIfMissing = true)
public class DaprContainerSpringBootDefaultsAutoConfiguration {

  @Bean
  static DaprContainerSpringBootDefaultsPostProcessor daprContainerSpringBootDefaultsPostProcessor() {
    return new DaprContainerSpringBootDefaultsPostProcessor();
  }
}
