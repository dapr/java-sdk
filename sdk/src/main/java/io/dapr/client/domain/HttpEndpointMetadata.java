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
 * HttpEndpointMetadata describes a registered Dapr HTTP endpoint.
 */
public final class HttpEndpointMetadata {

  private final String name;

  /**
   * Constructor for a HttpEndpointMetadata.
   *
   * @param name of the HTTP endpoint
   */
  public HttpEndpointMetadata(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
