/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client.domain;

/**
 * This class reprent what a State is
 * @param <T>
 */
public class StateKeyValue<T> {
  /**
   * The value of the state
   */
  private final T value;
  /**
   * The key of the state
   */
  private final String key;
  /**
   * The ETag to be used
   * For REDIS ONLY this must be an integer
   */
  private final String etag;

  /**
   * Create an inmutable state
   * @param value
   * @param key
   * @param etag
   */
  public StateKeyValue(T value, String key, String etag) {
    this.value = value;
    this.key = key;
    this.etag = etag;
  }

  /**
   * Retrieves the Value of the state
   * @return
   */
  public T getValue() {
    return value;
  }

  /**
   * Retrieves the Key of the state
   * @return
   */
  public String getKey() {
    return key;
  }

  /**
   * Retrieve the ETag of this state
   * @return
   */
  public String getEtag() {
    return etag;
  }
}
