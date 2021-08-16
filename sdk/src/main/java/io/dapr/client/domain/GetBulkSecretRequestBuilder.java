/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to fetch all secrets of a secret store.
 * Deprecated in favor of @see{@link GetBulkSecretRequest}.
 * Deprecated since SDK version 1.3.0, slated for Removal in SDK version 1.5.0.
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
