/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.List;

import io.grpc.Context;

/**
 * A request to get bulk state by keys.
 */
public class GetBulkStateRequest {

  private String stateStoreName;

  private List<String> keys;

  private StateOptions stateOptions;

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

  public StateOptions getStateOptions() {
    return stateOptions;
  }

  void setStateOptions(StateOptions stateOptions) {
    this.stateOptions = stateOptions;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }
}
