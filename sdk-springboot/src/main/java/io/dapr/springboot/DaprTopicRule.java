/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import java.util.Objects;

public class DaprTopicRule {
  private final String match;
  private final String path;

  public DaprTopicRule(String match, String path) {
    this.match = match;
    this.path = path;
  }

  public String getMatch() {
    return match;
  }

  public String getPath() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DaprTopicRule that = (DaprTopicRule) o;
    return match.equals(that.match) && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(match, path);
  }
}
