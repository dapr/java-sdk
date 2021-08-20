/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A request to save states to state store.
 */
public class SaveStateRequest {

  private final String storeName;

  private List<State<?>> states;

  public SaveStateRequest(String storeName) {
    this.storeName = storeName;
  }

  public String getStoreName() {
    return storeName;
  }

  public List<State<?>> getStates() {
    return states;
  }

  public SaveStateRequest setStates(List<State<?>> states) {
    this.states = states == null ? null : Collections.unmodifiableList(states);
    return this;
  }

  public SaveStateRequest setStates(State<?>... states) {
    this.states = Collections.unmodifiableList(Arrays.asList(states));
    return this;
  }
}
