/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.opentelemetry.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds a request to request states.
 */
public class GetStatesRequestBuilder {

  private final String stateStoreName;

  private final List<String> keys;

  private int parallelism = 1;

  private Context context;

  public GetStatesRequestBuilder(String stateStoreName, List<String> keys) {
    this.stateStoreName = stateStoreName;
    this.keys = keys == null ? null : Collections.unmodifiableList(keys);
  }

  public GetStatesRequestBuilder(String stateStoreName, String... keys) {
    this.stateStoreName = stateStoreName;
    this.keys = keys == null ? null : Collections.unmodifiableList(Arrays.asList(keys));
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
    request.setParallelism(this.parallelism);
    request.setContext(this.context);
    return request;
  }

}
