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

/**
 * Configuration settings for Dapr API logging.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#logging">
 *   Dapr logging configuration</a>
 */
public class ApiLoggingConfigurationSettings implements ConfigurationSettings {
  private final Boolean enabled;
  private final Boolean obfuscateUrls;
  private final Boolean omitHealthChecks;

  /**
   * Creates a new API logging configuration.
   *
   * @param enabled if true, enables API logging (default value for the {@code --enable-api-logging} daprd flag).
   */
  public ApiLoggingConfigurationSettings(Boolean enabled) {
    this(enabled, null, null);
  }

  /**
   * Creates a new API logging configuration.
   *
   * @param enabled          if true, enables API logging (default value for the {@code --enable-api-logging}
   *                         daprd flag).
   * @param obfuscateUrls    if true, obfuscates the values of URLs in HTTP API logs, logging the abstract route
   *                         name rather than the full path being invoked.
   * @param omitHealthChecks if true, calls to health check endpoints (e.g. {@code /v1.0/healthz}) are not logged
   *                         when API logging is enabled.
   */
  public ApiLoggingConfigurationSettings(Boolean enabled, Boolean obfuscateUrls, Boolean omitHealthChecks) {
    this.enabled = enabled;
    this.obfuscateUrls = obfuscateUrls;
    this.omitHealthChecks = omitHealthChecks;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Boolean getObfuscateUrls() {
    return obfuscateUrls;
  }

  public Boolean getOmitHealthChecks() {
    return omitHealthChecks;
  }
}
