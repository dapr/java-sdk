/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.opentelemetry.context.Context;

import java.util.List;

/**
 * A request to get bulk state by keys.
 */
public class GetStatesRequest {

  private String stateStoreName;

  private List<String> keys;

  private int parallelism;

  private Context context;

  public String getStateStoreName() {
    return stateStoreName;
  }

  void setStateStoreName(String stateStoreName) {
    this.stateStoreName = stateStoreName;
  }

  public List<String> getKeys() {
    return keys;
  }

  void setKeys(List<String> keys) {
    this.keys = keys;
  }

  public int getParallelism() {
    return parallelism;
  }

  void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }
}
