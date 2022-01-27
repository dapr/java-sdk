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
import java.util.Map;

/**
 * A request to invoke binding.
 */
public class InvokeBindingRequest {

  private final String name;

  private final String operation;

  private Object data;

  private Map<String, String> metadata;

  /**
   * Constructor for InvokeBindingRequest.
   *
   * @param bindingName Name of the binding
   * @param operation   Name of the binding operation
   */
  public InvokeBindingRequest(String bindingName, String operation) {
    this.name = bindingName;
    this.operation = operation;
  }

  public String getName() {
    return name;
  }

  public String getOperation() {
    return operation;
  }

  public Object getData() {
    return data;
  }

  public InvokeBindingRequest setData(Object data) {
    this.data = data;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public InvokeBindingRequest setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }
}
