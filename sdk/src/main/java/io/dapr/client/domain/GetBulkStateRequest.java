/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.List;
import java.util.Map;

/**
 * A request to get bulk state by keys.
 */
public class GetBulkStateRequest {

  private String storeName;

  private List<String> keys;

  private Map<String, String> metadata;

  private int parallelism;

  private Context context;

  public String getStoreName() {
    return storeName;
  }

  void setStoreName(String storeName) {
    this.storeName = storeName;
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

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
