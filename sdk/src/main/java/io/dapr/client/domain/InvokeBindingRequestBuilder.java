/*
 * Copyright 2021 The Dapr Authors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a request to publish an event.
 * Deprecated in favor of @see{@link InvokeBindingRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
public class InvokeBindingRequestBuilder {

  private final String name;

  private final String operation;

  private Object data;

  private Map<String, String> metadata = new HashMap<>();

  public InvokeBindingRequestBuilder(String name, String operation) {
    this.name = name;
    this.operation = operation;
  }

  public InvokeBindingRequestBuilder withData(Object data) {
    this.data = data;
    return this;
  }

  public InvokeBindingRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public InvokeBindingRequest build() {
    InvokeBindingRequest request = new InvokeBindingRequest(name, operation);
    return request.setData(this.data)
        .setMetadata(this.metadata);
  }

}
