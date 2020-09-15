/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.grpc.Context;
import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to publish an event.
 */
public class GetStateRequestBuilder {

  private final String stateStoreName;

  private final String key;

  private Map<String, String> metadata;

  private String etag;

  private StateOptions stateOptions;

  private Context context;

  public GetStateRequestBuilder(String stateStoreName, String key) {
    this.stateStoreName = stateStoreName;
    this.key = key;
  }

  public GetStateRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetStateRequestBuilder withEtag(String etag) {
    this.etag = etag;
    return this;
  }

  public GetStateRequestBuilder withStateOptions(StateOptions stateOptions) {
    this.stateOptions = stateOptions;
    return this;
  }

  public GetStateRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public GetStateRequest build() {
    GetStateRequest request = new GetStateRequest();
    request.setStateStoreName(this.stateStoreName);
    request.setKey(this.key);
    request.setMetadata(this.metadata);
    request.setEtag(this.etag);
    request.setStateOptions(this.stateOptions);
    request.setContext(this.context);
    return request;
  }

}
