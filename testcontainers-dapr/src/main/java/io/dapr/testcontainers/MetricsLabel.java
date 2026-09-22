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
import java.util.Map;

/**
 * A label filter used by a {@link MetricsRule}.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#metrics">
 *   Dapr metrics configuration</a>
 */
public class MetricsLabel {
  private final String name;
  private final Map<String, String> regex;

  /**
   * Creates a new metrics label filter.
   *
   * @param name  name of the metric label to filter on (e.g. {@code method}).
   * @param regex map of replacement value to regex expression. Label values matching the regex are replaced by
   *              the map key (e.g. {@code "orders/" -> "orders/.+"}).
   */
  public MetricsLabel(String name, Map<String, String> regex) {
    this.name = name;
    this.regex = regex != null ? Collections.unmodifiableMap(regex) : null;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getRegex() {
    return regex;
  }
}
