/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.springboot;

import java.util.Objects;

class DaprTopicRule {
  private final String match;
  private final String path;

  DaprTopicRule(String match, String path) {
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
