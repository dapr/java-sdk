/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to publish an event.
 */
public class GetSecretRequestBuilder {

  private final String storeName;

  private final String key;

  private Map<String, String> metadata;

  private Context context;

  public GetSecretRequestBuilder(String storeName, String key) {
    this.storeName = storeName;
    this.key = key;
  }

  public GetSecretRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetSecretRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public GetSecretRequest build() {
    GetSecretRequest request = new GetSecretRequest();
    request.setStoreName(this.storeName);
    request.setKey(this.key);
    request.setMetadata(this.metadata);
    request.setContext(this.context);
    return request;
  }

}
