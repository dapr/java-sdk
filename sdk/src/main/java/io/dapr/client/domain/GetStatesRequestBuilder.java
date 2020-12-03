/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.opentelemetry.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a request to request states.
 */
public class GetStatesRequestBuilder {

  private final String stateStoreName;

  private final List<String> keys;

  private Map<String, String> metadata;

  private int parallelism = 1;

  private Context context;

  public GetStatesRequestBuilder(String stateStoreName, List<String> keys) {
    this.stateStoreName = stateStoreName;
    this.keys = Collections.unmodifiableList(keys);
  }

  public GetStatesRequestBuilder(String stateStoreName, String... keys) {
    this.stateStoreName = stateStoreName;
    this.keys = Collections.unmodifiableList(Arrays.asList(keys));
  }

  public GetStatesRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public GetStatesRequestBuilder withParallelism(int parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  public GetStatesRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public GetStatesRequest build() {
    GetStatesRequest request = new GetStatesRequest();
    request.setStateStoreName(this.stateStoreName);
    request.setKeys(this.keys);
    request.setMetadata(this.metadata);
    request.setParallelism(this.parallelism);
    request.setContext(this.context);
    return request;
  }

}
