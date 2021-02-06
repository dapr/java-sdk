/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to request state.
 */
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
   * @return Request object.
   */
  public GetStateRequest build() {
    GetStateRequest request = new GetStateRequest();
    request.setStoreName(this.storeName);
    request.setKey(this.key);
    request.setMetadata(this.metadata);
    request.setStateOptions(this.stateOptions);
    return request;
  }

}
