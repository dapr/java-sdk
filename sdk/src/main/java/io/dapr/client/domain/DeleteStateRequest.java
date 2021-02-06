/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * A request to delete a state by key.
 */
public class DeleteStateRequest {

  private String stateStoreName;

  private String key;

  private Map<String, String> metadata;

  private String etag;

  private StateOptions stateOptions;

  public String getStateStoreName() {
    return stateStoreName;
  }

  void setStateStoreName(String stateStoreName) {
    this.stateStoreName = stateStoreName;
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

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? Collections.emptyMap() : metadata;
  }
}
