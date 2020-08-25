/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.grpc.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A request to save states to state store.
 */
public class SaveStateRequestBuilder {

  private final String stateStoreName;

  private List<State<?>> states = new ArrayList<>();

  private Context context;

  public SaveStateRequestBuilder(String stateStoreName) {
    this.stateStoreName = stateStoreName;
  }

  public SaveStateRequestBuilder withStates(State<?>... states) {
    this.states = Collections.unmodifiableList(Arrays.asList(states));
    return this;
  }

  public SaveStateRequestBuilder withStates(List<State<?>> states) {
    this.states = states == null ? null : Collections.unmodifiableList(states);
    return this;
  }

  public SaveStateRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public SaveStateRequest build() {
    SaveStateRequest request = new SaveStateRequest();
    request.setStateStoreName(this.stateStoreName);
    request.setStates(this.states);
    request.setContext(this.context);
    return request;
  }
}
