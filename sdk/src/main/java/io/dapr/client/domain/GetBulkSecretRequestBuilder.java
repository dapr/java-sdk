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
 * Builds a request to fetch all secrets of a secret store.
 * Deprecated in favor of @see{@link GetBulkSecretRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0.
 */
@Deprecated
public class GetBulkSecretRequestBuilder {

  private final String storeName;

  private Map<String, String> metadata;

  public GetBulkSecretRequestBuilder(String storeName) {
    this.storeName = storeName;
  }

  public GetBulkSecretRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public GetBulkSecretRequest build() {
    GetBulkSecretRequest request = new GetBulkSecretRequest(storeName);
    return request.setMetadata(this.metadata);
  }

}
