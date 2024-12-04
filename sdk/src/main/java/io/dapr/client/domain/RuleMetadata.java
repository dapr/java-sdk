/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.client.domain;

/**
 * RuleMetadata describes the Subscription Rule's Metadata.
 */
public final class RuleMetadata {

  private final String match;
  private final String path;

  /**
   * Constructor for a RuleMetadata.
   *
   * @param match CEL expression to match the message
   * @param path path to route the message
   */
  public RuleMetadata(String match, String path) {
    this.match = match;
    this.path = path;
  }

  public String getMatch() {
    return match;
  }

  public String getPath() {
    return path;
  }

}
