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
 * Configuration settings for HTTP metrics in the Dapr runtime.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#metrics">
 *   Dapr metrics configuration</a>
 */
public class HttpMetricsConfigurationSettings implements ConfigurationSettings {
  private final Boolean increasedCardinality;
  private final List<String> pathMatching;
  private final Boolean excludeVerbs;

  /**
   * Creates a new HTTP metrics configuration.
   *
   * @param increasedCardinality if true (the default), each request path generates a new metrics bucket.
   *                             Set to false to manage cardinality and avoid high memory usage when there
   *                             are many distinct endpoints.
   */
  public HttpMetricsConfigurationSettings(Boolean increasedCardinality) {
    this(increasedCardinality, null, null);
  }

  /**
   * Creates a new HTTP metrics configuration.
   *
   * @param increasedCardinality if true (the default), each request path generates a new metrics bucket.
   *                             Set to false to manage cardinality and avoid high memory usage when there
   *                             are many distinct endpoints.
   * @param pathMatching         paths (with optional parameters, e.g. {@code /orders/{orderID}}) used to
   *                             match request paths so cardinality can be managed.
   * @param excludeVerbs         if true (default is false), the Dapr HTTP server ignores the request HTTP verb
   *                             when building the method metric label.
   */
  public HttpMetricsConfigurationSettings(
      Boolean increasedCardinality,
      List<String> pathMatching,
      Boolean excludeVerbs
  ) {
    this.increasedCardinality = increasedCardinality;
    this.pathMatching = pathMatching != null ? Collections.unmodifiableList(pathMatching) : null;
    this.excludeVerbs = excludeVerbs;
  }

  public Boolean getIncreasedCardinality() {
    return increasedCardinality;
  }

  public List<String> getPathMatching() {
    return pathMatching;
  }

  public Boolean getExcludeVerbs() {
    return excludeVerbs;
  }
}
