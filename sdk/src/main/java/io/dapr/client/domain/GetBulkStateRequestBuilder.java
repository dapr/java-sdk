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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a request to request states.
 * Deprecated in favor of @see{@link GetBulkStateRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0.
 */
@Deprecated
public class GetBulkStateRequestBuilder {

  private final String storeName;

  private final List<String> keys;

  private Map<String, String> metadata;

  private int parallelism = 1;

  public GetBulkStateRequestBuilder(String storeName, List<String> keys) {
    this.storeName = storeName;
    this.keys = keys == null ? null : Collections.unmodifiableList(keys);
  }

  public GetBulkStateRequestBuilder(String storeName, String... keys) {
    this.storeName = storeName;
    this.keys = keys == null ? null : Collections.unmodifiableList(Arrays.asList(keys));
  }

  public GetBulkStateRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetBulkStateRequestBuilder withParallelism(int parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public GetBulkStateRequest build() {
    GetBulkStateRequest request = new GetBulkStateRequest(storeName, keys);
    return request.setMetadata(this.metadata)
        .setParallelism(this.parallelism);
  }

}
