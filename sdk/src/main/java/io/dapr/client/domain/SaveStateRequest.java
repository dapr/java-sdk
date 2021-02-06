/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.List;

/**
 * A request to save states to state store.
 */
public class SaveStateRequest {

  private String storeName;

  private List<State<?>> states;

  public String getStoreName() {
    return storeName;
  }

  void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public List<State<?>> getStates() {
    return states;
  }

  void setStates(List<State<?>> states) {
    this.states = states;
  }
}
