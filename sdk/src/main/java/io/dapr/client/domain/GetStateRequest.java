/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.opentelemetry.context.Context;

import java.util.Collections;
import java.util.Map;

/**
 * A request to get a state by key.
 */
public class GetStateRequest {

  private String storeName;

  private String key;

  private Map<String, String> metadata;

  private String etag;

  private StateOptions stateOptions;

  private Context context;

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

  public String getEtag() {
    return etag;
  }

  void setEtag(String etag) {
    this.etag = etag;
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

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? Collections.emptyMap() : metadata;
  }
}
