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
   * Keep in mind that for some state stores (like reids) only numbers are supported.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StateKeyValue)) return false;

    StateKeyValue<?> that = (StateKeyValue<?>) o;

    if (getValue() != null ? !getValue().equals(that.getValue()) : that.getValue() != null) return false;
    if (getKey() != null ? !getKey().equals(that.getKey()) : that.getKey() != null) return false;
    if (getEtag() != null ? !getEtag().equals(that.getEtag()) : that.getEtag() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getValue() != null ? getValue().hashCode() : 0;
    result = 31 * result + (getKey() != null ? getKey().hashCode() : 0);
    result = 31 * result + (getEtag() != null ? getEtag().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "StateKeyValue{" +
        "value=" + value +
        ", key='" + key + '\'' +
        ", etag='" + etag + '\'' +
        '}';
  }
}
