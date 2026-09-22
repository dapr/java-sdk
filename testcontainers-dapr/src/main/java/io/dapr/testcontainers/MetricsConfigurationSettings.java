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
import java.util.List;

/**
 * Configuration settings for how metrics are collected and exposed by the Dapr runtime.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#metrics">
 *   Dapr metrics configuration</a>
 */
public class MetricsConfigurationSettings implements ConfigurationSettings {
  private final Boolean enabled;
  private final List<MetricsRule> rules;
  private final List<Integer> latencyDistributionBuckets;
  private final HttpMetricsConfigurationSettings http;
  private final Boolean recordErrorCodes;

  /**
   * Creates a new metrics configuration.
   *
   * @param enabled if true (the default), enables metrics collection and the metrics endpoint.
   */
  public MetricsConfigurationSettings(Boolean enabled) {
    this(enabled, null, null, null, null);
  }

  /**
   * Creates a new metrics configuration.
   *
   * @param enabled if true (the default), enables metrics collection and the metrics endpoint.
   * @param http    HTTP metrics configuration settings.
   */
  public MetricsConfigurationSettings(Boolean enabled, HttpMetricsConfigurationSettings http) {
    this(enabled, null, null, http, null);
  }

  /**
   * Creates a new metrics configuration.
   *
   * @param enabled                    if true (the default), enables metrics collection and the metrics endpoint.
   * @param rules                      named rules to filter metrics. Each rule contains a set of labels to filter
   *                                   on and a regex expression to apply to the metrics path.
   * @param latencyDistributionBuckets latency distribution buckets in milliseconds for latency metrics
   *                                   histograms.
   * @param http                       HTTP metrics configuration settings.
   * @param recordErrorCodes           if true, enables recording of error code metrics (disabled by default).
   */
  public MetricsConfigurationSettings(
      Boolean enabled,
      List<MetricsRule> rules,
      List<Integer> latencyDistributionBuckets,
      HttpMetricsConfigurationSettings http,
      Boolean recordErrorCodes
  ) {
    this.enabled = enabled;
    this.rules = rules != null ? Collections.unmodifiableList(rules) : null;
    this.latencyDistributionBuckets = latencyDistributionBuckets != null
        ? Collections.unmodifiableList(latencyDistributionBuckets)
        : null;
    this.http = http;
    this.recordErrorCodes = recordErrorCodes;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public List<MetricsRule> getRules() {
    return rules;
  }

  public List<Integer> getLatencyDistributionBuckets() {
    return latencyDistributionBuckets;
  }

  public HttpMetricsConfigurationSettings getHttp() {
    return http;
  }

  public Boolean getRecordErrorCodes() {
    return recordErrorCodes;
  }
}
