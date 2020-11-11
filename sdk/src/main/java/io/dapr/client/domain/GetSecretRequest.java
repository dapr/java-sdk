/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;


import io.opentelemetry.context.Context;

import java.util.Map;

/**
 * A request to get a secret by key.
 */
public class GetSecretRequest {

  private String secretStoreName;

  private String key;

  private Map<String, String> metadata;

  private Context context;

  public String getSecretStoreName() {
    return secretStoreName;
  }

  void setSecretStoreName(String secretStoreName) {
    this.secretStoreName = secretStoreName;
  }

  public String getKey() {
    return key;
  }

  void setKey(String key) {
    this.key = key;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }
}
