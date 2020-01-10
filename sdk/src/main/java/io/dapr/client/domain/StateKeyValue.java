/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client.domain;

public class StateKeyValue<T> {
  private final T value;
  private final String key;
  private final String etag;

  public StateKeyValue(T value, String key, String etag) {
    this.value = value;
    this.key = key;
    this.etag = etag;
  }

  public T getValue() {
    return value;
  }

  public String getKey() {
    return key;
  }

  public String getEtag() {
    return etag;
  }
}
