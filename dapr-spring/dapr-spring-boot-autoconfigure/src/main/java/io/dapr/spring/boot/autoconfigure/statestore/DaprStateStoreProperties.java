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

package io.dapr.spring.boot.autoconfigure.statestore;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = DaprStateStoreProperties.CONFIG_PREFIX)
public class DaprStateStoreProperties {

  public static final String CONFIG_PREFIX = "dapr.statestore";

  /**
   * Name of the StateStore Dapr component.
   */
  private String name;
  private String binding;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBinding() {
    return binding;
  }

  public void setBinding(String binding) {
    this.binding = binding;
  }
}
