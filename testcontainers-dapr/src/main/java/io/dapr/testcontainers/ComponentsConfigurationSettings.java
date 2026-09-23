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

package io.dapr.testcontainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration settings for the component types the Dapr runtime is allowed to load.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#disallow-usage-of-certain-component-types">
 *   Dapr disallow usage of certain component types</a>
 */
public class ComponentsConfigurationSettings implements ConfigurationSettings {
  private final List<String> deny;

  /**
   * Creates a new components configuration.
   *
   * @param deny component types the Dapr runtime must not initialize, in the format {@code {type}.{name}}
   *             (e.g. "bindings.smtp") or {@code {type}.{name}/{version}} (e.g. "state.redis/v1").
   */
  public ComponentsConfigurationSettings(List<String> deny) {
    this.deny = deny != null ? Collections.unmodifiableList(new ArrayList<>(deny)) : null;
  }

  public List<String> getDeny() {
    return deny;
  }
}
