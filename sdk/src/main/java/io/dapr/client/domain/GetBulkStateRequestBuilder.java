/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a request to request states.
 */
public class GetBulkStateRequestBuilder {

  private final String storeName;

  private final List<String> keys;

  private Map<String, String> metadata;

  private int parallelism = 1;

  private Context context;

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

  public GetBulkStateRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public GetBulkStateRequest build() {
    GetBulkStateRequest request = new GetBulkStateRequest();
    request.setStoreName(this.storeName);
    request.setKeys(this.keys);
    request.setMetadata(this.metadata);
    request.setParallelism(this.parallelism);
    request.setContext(this.context);
    return request;
  }

}
