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
 * A named rule used to filter Dapr metrics.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#metrics">
 *   Dapr metrics configuration</a>
 */
public class MetricsRule {
  private final String name;
  private final List<MetricsLabel> labels;

  /**
   * Creates a new metrics rule.
   *
   * @param name   name of the metric the rule applies to
   *               (e.g. {@code dapr_runtime_service_invocation_req_sent_total}).
   * @param labels labels to filter on, each with a regex expression applied to the metrics path.
   */
  public MetricsRule(String name, List<MetricsLabel> labels) {
    this.name = name;
    this.labels = labels != null ? Collections.unmodifiableList(labels) : null;
  }

  public String getName() {
    return name;
  }

  public List<MetricsLabel> getLabels() {
    return labels;
  }
}
