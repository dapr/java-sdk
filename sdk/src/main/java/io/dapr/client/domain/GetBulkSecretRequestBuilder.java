/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to fetch all secrets of a secret store.
 */
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
   * @return Request object.
   */
  public GetBulkSecretRequest build() {
    GetBulkSecretRequest request = new GetBulkSecretRequest();
    request.setStoreName(this.storeName);
    request.setMetadata(this.metadata);
    return request;
  }

}
