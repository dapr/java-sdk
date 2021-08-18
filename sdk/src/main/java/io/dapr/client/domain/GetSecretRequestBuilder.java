/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to publish an event.
 * Deprecated in favor of @see{@link GetSecretRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
public class GetSecretRequestBuilder {

  private final String storeName;

  private final String key;

  private Map<String, String> metadata;

  public GetSecretRequestBuilder(String storeName, String key) {
    this.storeName = storeName;
    this.key = key;
  }

  public GetSecretRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public GetSecretRequest build() {
    GetSecretRequest request = new GetSecretRequest(storeName, key);
    return request.setMetadata(this.metadata);
  }

}
