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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration settings for the name resolution component used by the Dapr runtime for service invocation.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#name-resolution-component">
 *   Dapr name resolution configuration</a>
 */
public class NameResolutionConfigurationSettings implements ConfigurationSettings {
  private final String component;
  private final String version;
  private final Map<String, Object> configuration;

  /**
   * Creates a new name resolution configuration.
   *
   * @param component name of the name resolution component to use for service invocation (e.g. "mdns", "sqlite").
   */
  public NameResolutionConfigurationSettings(String component) {
    this(component, null, null);
  }

  /**
   * Creates a new name resolution configuration.
   *
   * @param component name of the name resolution component to use for service invocation (e.g. "mdns", "sqlite").
   * @param version   version of the selected name resolution component (e.g. "v1").
   */
  public NameResolutionConfigurationSettings(String component, String version) {
    this(component, version, null);
  }

  /**
   * Creates a new name resolution configuration.
   *
   * @param component     name of the name resolution component to use for service invocation
   *                      (e.g. "mdns", "sqlite").
   * @param version       version of the selected name resolution component (e.g. "v1").
   * @param configuration metadata configuration specific to the chosen name resolution component.
   */
  public NameResolutionConfigurationSettings(String component, String version, Map<String, Object> configuration) {
    this.component = component;
    this.version = version;
    this.configuration = configuration != null
        ? Collections.unmodifiableMap(new LinkedHashMap<>(configuration))
        : null;
  }

  public String getComponent() {
    return component;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }
}
