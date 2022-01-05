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
 * Builds a request to request state.
 * Deprecated in favor of @see{@link GetStateRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
public class GetStateRequestBuilder {

  private final String storeName;

  private final String key;

  private Map<String, String> metadata;

  private StateOptions stateOptions;

  public GetStateRequestBuilder(String storeName, String key) {
    this.storeName = storeName;
    this.key = key;
  }

  public GetStateRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetStateRequestBuilder withStateOptions(StateOptions stateOptions) {
    this.stateOptions = stateOptions;
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public GetStateRequest build() {
    GetStateRequest request = new GetStateRequest(storeName, key);
    return request.setMetadata(this.metadata)
        .setStateOptions(this.stateOptions);
  }

}
