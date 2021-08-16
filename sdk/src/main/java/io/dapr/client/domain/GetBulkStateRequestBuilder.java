/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a request to request states.
 * Deprecated in favor of @see{@link GetBulkStateRequest}.
 * Deprecated since SDK version 1.3.0, slated for Removal in SDK version 1.5.0.
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
