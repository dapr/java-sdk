/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to fetch all secrets of a secret store.
 */
public class GetBulkSecretRequestBuilder {

  private final String storeName;

  private Map<String, String> metadata;

  private Context context;

  public GetBulkSecretRequestBuilder(String storeName) {
    this.storeName = storeName;
  }

  public GetBulkSecretRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetBulkSecretRequestBuilder withContext(Context context) {
    this.context = context;
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
    request.setContext(this.context);
    return request;
  }

}
