/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

/**
 * This class reprent what a State is.
 *
 * @param <T> The type of the value of the sate
 */
public class State<T> {
  /**
   * The value of the state.
   */
  private final T value;
  /**
   * The key of the state.
   */
  private final String key;
  /**
   * The ETag to be used
   * Keep in mind that for some state stores (like reids) only numbers are supported.
   */
  private final String etag;

  /**
   * The options used for saving the state.
   */
  private final StateOptions options;

  /**
   * Create an immutable state
   * This Constructor MUST be used anytime you need to retrieve or delete a State.
   *
   * @param key     - The key of the state
   * @param etag    - The etag of the state - Keep in mind that for some state stores (like redis) only numbers
   *                are supported.
   * @param options - REQUIRED when saving a state.
   */
  public State(String key, String etag, StateOptions options) {
    this.value = null;
    this.key = key;
    this.etag = etag;
    this.options = options;
  }

  /**
   * Create an inmutable state.
   * This Constructor MUST be used anytime you want the state to be send for a Save operation.
   *
   * @param value   - The value of the state.
   * @param key     - The key of the state.
   * @param etag    - The etag of the state - Keep in mind that for some state stores (like redis)
   *                only numbers are supported.
   * @param options - REQUIRED when saving a state.
   */
  public State(T value, String key, String etag, StateOptions options) {
    this.value = value;
    this.key = key;
    this.etag = etag;
    this.options = options;
  }

  /**
   * Retrieves the Value of the state.
   *
   * @return The value of the state
   */
  public T getValue() {
    return value;
  }

  /**
   * Retrieves the Key of the state.
   *
   * @return The key of the state
   */
  public String getKey() {
    return key;
  }

  /**
   * Retrieve the ETag of this state.
   *
   * @return The etag of the state
   */
  public String getEtag() {
    return etag;
  }

  /**
   * Retrieve the Options used for saving the state.
   *
   * @return The options to save the state
   */
  public StateOptions getOptions() {
    return options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof State)) {
      return false;
    }

    State<?> that = (State<?>) o;

    if (getValue() != null ? !getValue().equals(that.getValue()) : that.getValue() != null) {
      return false;
    }

    if (getKey() != null ? !getKey().equals(that.getKey()) : that.getKey() != null) {
      return false;
    }

    if (getEtag() != null ? !getEtag().equals(that.getEtag()) : that.getEtag() != null) {
      return false;
    }

    if (getOptions() != null ? !getOptions().equals(that.getOptions()) : that.getOptions() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = getValue() != null ? getValue().hashCode() : 0;
    result = 31 * result + (getKey() != null ? getKey().hashCode() : 0);
    result = 31 * result + (getEtag() != null ? getEtag().hashCode() : 0);
    result = 31 * result + (getOptions() != null ? options.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "StateKeyValue{"
        + "value=" + value
        + ", key='" + key + "'"
        + ", etag='" + etag + "'"
        + ", options={'" + options != null ? options.toString() : "null" + "}"
        + "}";
  }
}
