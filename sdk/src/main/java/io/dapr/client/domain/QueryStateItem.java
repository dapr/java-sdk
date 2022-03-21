/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client.domain;


public class QueryStateItem<T> {


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
   * Keep in mind that for some state stores (like redis) only numbers are supported.
   */
  private final String etag;

  /**
   * The error in case the key could not be retrieved.
   */
  private final String error;

  /**
   * Create an immutable state reference to be retrieved or deleted.
   * This Constructor CAN be used anytime you need to retrieve or delete a state.
   *
   * @param key - The key of the state
   */
  public QueryStateItem(String key) {
    this.key = key;
    this.value = null;
    this.etag = null;
    this.error = null;
  }

  /**
   * Create an immutable state reference to be retrieved or deleted.
   * This Constructor CAN be used anytime you need to retrieve or delete a state.
   *
   * @param key   - The key of the state
   * @param etag  - The etag of the state - Keep in mind that for some state stores (like redis) only numbers
   *              are supported.
   * @param error - Error when fetching the state.
   */
  public QueryStateItem(String key, String etag, String error) {
    this.value = null;
    this.key = key;
    this.etag = etag;
    this.error = error;
  }

  /**
   * Create an immutable state.
   * This Constructor CAN be used anytime you want the state to be saved.
   *
   * @param key   - The key of the state.
   * @param value - The value of the state.
   * @param etag  - The etag of the state - for some state stores (like redis) only numbers are supported.
   */
  public QueryStateItem(String key, T value, String etag) {
    this.value = value;
    this.key = key;
    this.etag = etag;
    this.error = null;
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
   * Retrieve the error for this state.
   *
   * @return The error for this state.
   */

  public String getError() {
    return error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof QueryStateItem)) {
      return false;
    }

    QueryStateItem<?> that = (QueryStateItem<?>) o;

    if (getValue() != null ? !getValue().equals(that.getValue()) : that.getValue() != null) {
      return false;
    }

    if (getKey() != null ? !getKey().equals(that.getKey()) : that.getKey() != null) {
      return false;
    }

    if (getEtag() != null ? !getEtag().equals(that.getEtag()) : that.getEtag() != null) {
      return false;
    }

    return getError() != null ? getError().equals(that.getError()) : that.getError() == null;
  }

  @Override
  public int hashCode() {
    int result = getValue() != null ? getValue().hashCode() : 0;
    result = 31 * result + (getKey() != null ? getKey().hashCode() : 0);
    result = 31 * result + (getEtag() != null ? getEtag().hashCode() : 0);
    result = 31 * result + (getError() != null ? getError().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "QueryStateItem{"
        + "key='" + key + "'"
        + ", value=" + value
        + ", etag='" + etag + "'"
        + ", error='" + error + "'"
        + "}";
  }
}
