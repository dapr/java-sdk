/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;


import io.opentelemetry.context.Context;

import java.util.List;

/**
 * A request to save states to state store.
 */
public class SaveStateRequest {

  private String stateStoreName;

  private List<State<?>> states;

  private Context context;

  public String getStateStoreName() {
    return stateStoreName;
  }

  void setStateStoreName(String stateStoreName) {
    this.stateStoreName = stateStoreName;
  }

  public List<State<?>> getStates() {
    return states;
  }

  void setStates(List<State<?>> states) {
    this.states = states;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }
}
