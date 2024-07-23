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

import io.dapr.client.DaprClientBuilder;
import io.dapr.spring.core.client.DaprClientCustomizer;

import java.util.List;

/**
 * Builder for configuring a {@link DaprClientBuilder}.
 */
public class DaprClientBuilderConfigurer {

  private List<DaprClientCustomizer> customizers;

  void setDaprClientCustomizer(List<DaprClientCustomizer> customizers) {
    this.customizers = customizers;
  }

  /**
   * Configure the specified {@link DaprClientBuilder}. The builder can be further
   * tuned and default settings can be overridden.
   *
   * @param builder the {@link DaprClientBuilder} instance to configure
   * @return the configured builder
   */
  public DaprClientBuilder configure(DaprClientBuilder builder) {
    applyCustomizers(builder);
    return builder;
  }

  private void applyCustomizers(DaprClientBuilder builder) {
    if (this.customizers != null) {
      for (DaprClientCustomizer customizer : this.customizers) {
        customizer.customize(builder);
      }
    }
  }

}
