/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Map;

/**
 * A request to get a secret by key.
 */
public class GetBulkSecretRequest {

  private String storeName;

  private Map<String, String> metadata;

  public String getStoreName() {
    return storeName;
  }

  void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
