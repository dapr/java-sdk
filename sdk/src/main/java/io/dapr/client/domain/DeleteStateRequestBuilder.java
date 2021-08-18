/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Builds a request to delete a state by key.
 * Deprecated in favor of @see{@link DeleteStateRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0.
 */
@Deprecated
public class DeleteStateRequestBuilder {

  private final String stateStoreName;

  private final String key;

  private Map<String, String> metadata;

  private String etag;

  private StateOptions stateOptions;

  public DeleteStateRequestBuilder(String stateStoreName, String key) {
    this.stateStoreName = stateStoreName;
    this.key = key;
  }

  public DeleteStateRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public DeleteStateRequestBuilder withEtag(String etag) {
    this.etag = etag;
    return this;
  }

  public DeleteStateRequestBuilder withStateOptions(StateOptions stateOptions) {
    this.stateOptions = stateOptions;
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public DeleteStateRequest build() {
    DeleteStateRequest request = new DeleteStateRequest(this.stateStoreName, this.key);
    request.setMetadata(metadata);
    request.setEtag(this.etag);
    request.setStateOptions(this.stateOptions);
    return request;
  }

}
