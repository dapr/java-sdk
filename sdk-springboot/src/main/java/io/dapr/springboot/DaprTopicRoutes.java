/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public class DaprTopicRoutes {
  private final List<DaprTopicRule> rules;
  @JsonProperty("default")
  private final String defaultRoute;

  public DaprTopicRoutes(List<DaprTopicRule> rules, String defaultRoute) {
    this.rules = rules;
    this.defaultRoute = defaultRoute;
  }

  public List<DaprTopicRule> getRules() {
    return rules;
  }

  public String getDefaultRoute() {
    return defaultRoute;
  }
}
