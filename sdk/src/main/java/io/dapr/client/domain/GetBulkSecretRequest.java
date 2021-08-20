/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.Map;

/**
 * A request to get a secret by key.
 */
public class GetBulkSecretRequest {

  private final String storeName;

  private Map<String, String> metadata;

  public GetBulkSecretRequest(String storeName) {
    this.storeName = storeName;
  }

  public String getStoreName() {
    return storeName;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public GetBulkSecretRequest setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }
}
