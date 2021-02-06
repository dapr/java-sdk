/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * A request to get a state by key.
 */
public class GetStateRequest {

  private String storeName;

  private String key;

  private Map<String, String> metadata;

  private StateOptions stateOptions;

  public String getStoreName() {
    return storeName;
  }

  void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public String getKey() {
    return key;
  }

  void setKey(String key) {
    this.key = key;
  }

  public StateOptions getStateOptions() {
    return stateOptions;
  }

  void setStateOptions(StateOptions stateOptions) {
    this.stateOptions = stateOptions;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? Collections.emptyMap() : metadata;
  }
}
